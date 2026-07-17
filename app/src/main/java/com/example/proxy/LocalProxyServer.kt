package com.example.proxy

import com.example.agent.AgentOrchestrator
import com.example.agent.AgentStepLog
import com.example.data.repository.TokenRepository
import com.example.platform.PlatformRegistry
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*

class LocalProxyServer(
    private val port: Int,
    private val proxyApiKey: String = "sk-local-proxy-key",
    private val tokenRepository: TokenRepository,
    private val modelRouter: ModelRouter,
    private val apiForwarder: ApiForwarder,
    private val agentOrchestrator: AgentOrchestrator? = null
) {
    @Volatile
    private var server: ApplicationEngine? = null

    val agentLogs = mutableListOf<AgentStepLog>()

    fun start() {
        if (server != null) return
        server = embeddedServer(Netty, port = port) { module() }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun isRunning(): Boolean = server != null

    private fun Application.module() {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }

        routing {
            get("/") {
                call.respondText(
                    """{"service":"AI Token Relay","version":"2.5.0","features":["multi-platform proxy","agent/tool-calling (12 tools)","streaming SSE","parallel execution","git integration","code analysis","knowledge memory"],"endpoints":["/v1/models","/v1/chat/completions","/v1/agent/tools","/v1/agent/logs"]}""",
                    ContentType.Application.Json
                )
            }

            get("/health") { call.respondText("OK", ContentType.Text.Plain) }

            get("/v1/agent/tools") {
                if (!checkAuth(call)) return@get
                val tools = agentOrchestrator?.let {
                    """{"tools": ${PlatformRegistry.getAllModels().joinToString { "\"$it\"" }}}"""
                } ?: """{"tools":[]}"""
                call.respondText(tools, ContentType.Application.Json)
            }

            get("/v1/agent/logs") {
                if (!checkAuth(call)) return@get
                val logs = agentLogs.map {
                    JsonObject(mapOf(
                        "iteration" to JsonPrimitive(it.iteration),
                        "tool" to JsonPrimitive(it.toolName),
                        "args" to JsonPrimitive(it.arguments),
                        "error" to JsonPrimitive(it.isError),
                        "result" to JsonPrimitive(it.resultSummary)
                    ))
                }
                call.respondText(
                    JsonObject(mapOf("logs" to JsonArray(logs))).toString(),
                    ContentType.Application.Json
                )
            }

            get("/v1/models") {
                if (!checkAuth(call)) return@get
                val models = PlatformRegistry.getAllModels()
                val data = models.map {
                    JsonObject(mapOf("id" to JsonPrimitive(it), "object" to JsonPrimitive("model")))
                }
                call.respondText(
                    JsonObject(mapOf("object" to JsonPrimitive("list"), "data" to JsonArray(data))).toString(),
                    ContentType.Application.Json
                )
            }

            post("/v1/chat/completions") {
                if (!checkAuth(call)) return@post
                val body = call.receiveText()
                val json = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
                if (json == null) {
                    call.respondText("{\"error\":\"invalid json\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@post
                }

                val modelId = json["model"]?.jsonPrimitive?.content ?: run {
                    call.respondText("{\"error\":\"model field required\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@post
                }

                val route = modelRouter.route(modelId)
                if (route == null) {
                    call.respondText(
                        "{\"error\":\"no active token for model '$modelId'\"}",
                        ContentType.Application.Json,
                        HttpStatusCode.ServiceUnavailable
                    )
                    return@post
                }

                tokenRepository.updateLastUsed(route.token.id, System.currentTimeMillis())

                val hasTools = json["tools"] != null
                val isStream = json["stream"]?.jsonPrimitive?.booleanOrNull == true

                if (hasTools && agentOrchestrator != null) {
                    handleAgentRequest(call, route, modelId, json, isStream)
                } else if (isStream) {
                    handleStreamResponse(call, route, body)
                } else {
                    handleNormalResponse(call, route, body)
                }
            }
        }
    }

    private suspend fun handleAgentRequest(
        call: ApplicationCall,
        route: RouteResult,
        modelId: String,
        requestJson: JsonObject,
        clientWantsStream: Boolean
    ) {
        agentLogs.clear()

        val messages = requestJson["messages"]?.jsonArray ?: JsonArray(emptyList())
        val systemPrompt = """You are an advanced AI coding agent running on AI Relay v2.5 — a local proxy that empowers any OpenAI-compatible client with autonomous tool execution on Android.

=== AVAILABLE TOOLS (12 total) ===

CODE & FILES:
  shell_exec      — Run shell commands (30s timeout, dangerous ops require confirmation)
  file_read       — Read file contents with line count and size
  file_write      — Write/create files, auto-creates parent directories
  file_search     — Find files by glob pattern (e.g. "**/*.kt", "src/**/*.java")
  content_search  — Grep-like regex search across files with extension filter

VERSION CONTROL:
  git             — Full git: status/diff/log/branch/add/commit/blame/show/remote

ANALYSIS:
  code_analyze    — Parse source files for imports, classes, functions, dependency graphs
  device_info     — Report OS version, memory, storage, CPU, network interfaces

WEB & DATA:
  web_fetch       — HTTP GET/POST with headers, returns response body

WORKFLOW:
  todo_write      — Session task tracker: add/update/list/clear tasks
  clipboard       — Read/write device clipboard (for sharing code)
  knowledge       — Persistent knowledge store: save/recall/search across sessions

=== EXECUTION PROTOCOL ===

BEFORE ANY ACTION:
1. UNDERSTAND: Read relevant files with file_read, analyze structure with code_analyze
2. PLAN: Break complex tasks into steps using todo_write
3. SEARCH: Use file_search to locate files, content_search to find patterns
4. CHECK: Run git status before modifications, know the current state

DURING MODIFICATIONS:
5. EDIT: Use file_write for changes (the tool overwrites the file, provide full content)
6. VERIFY: After each change, run relevant checks (syntax, lint, test)
7. COMMIT: Use git add + git commit with conventional commit messages

AFTER COMPLETION:
8. CLEANUP: List completed tasks with todo_write list
9. SUMMARIZE: Briefly state what was changed and why

=== CRITICAL RULES ===

- Shell commands: Explain what and why BEFORE executing
- Parallel execution: file_read, file_search, content_search, device_info are auto-parallelized
- Dangerous commands DETECTED: rm -rf, chmod 777, git push --force, mount, iptables etc trigger confirmation
- Path convention: Use absolute paths, prefer /sdcard/workspace/ for project files
- Error recovery: If a tool fails, analyze error and try alternative approach
- Context budget: Max 80K tokens before auto-compression
- Iteration limit: 20 tool calls per session (plan accordingly)
- Knowledge persistence: Use knowledge save to remember project preferences and decisions
- Git discipline: Commit atomic changes with descriptive messages

=== RESPONSE STYLE ===

- Direct, concise, no fluff. Lead with action, not preamble.
- Show code output inline, not as explanation.
- When searching, report what was found and where.
- Never say "Certainly!" or "Let me break this down" — just do it.
- Use todo_write to demonstrate thoroughness on complex tasks."""

        val enhancedMessages = JsonArray(
            listOf(JsonObject(mapOf(
                "role" to JsonPrimitive("system"),
                "content" to JsonPrimitive(systemPrompt)
            ))) + messages.toList()
        )

        agentOrchestrator!!.onToolCall = { iter, tool, args ->
            agentLogs.add(AgentStepLog(iter.toIntOrNull() ?: 0, tool, args, false, ""))
        }

        val result = agentOrchestrator!!.executeLoop(route, enhancedMessages, modelId)

        val responseJson = JsonObject(mapOf(
            "id" to JsonPrimitive("agent-${System.currentTimeMillis()}"),
            "object" to JsonPrimitive("chat.completion"),
            "created" to JsonPrimitive(System.currentTimeMillis() / 1000),
            "model" to JsonPrimitive(modelId),
            "choices" to JsonArray(listOf(JsonObject(mapOf(
                "index" to JsonPrimitive(0),
                "message" to JsonObject(mapOf(
                    "role" to JsonPrimitive("assistant"),
                    "content" to JsonPrimitive(result.content)
                )),
                "finish_reason" to JsonPrimitive(if (result.success) "stop" else "length")
            )))),
            "usage" to JsonObject(mapOf(
                "prompt_tokens" to JsonPrimitive(0),
                "completion_tokens" to JsonPrimitive(0),
                "total_tokens" to JsonPrimitive(0),
                "agent_iterations" to JsonPrimitive(result.iterations),
                "agent_tool_calls" to JsonPrimitive(result.logs.size)
            ))
        )).toString()

        call.respondText(responseJson, ContentType.Application.Json, HttpStatusCode.OK)
    }

    private suspend fun handleNormalResponse(call: ApplicationCall, route: RouteResult, body: String) {
        val result = apiForwarder.forwardChatCompletion(route, body)
        val status = if (result.success) HttpStatusCode.OK else HttpStatusCode.fromValue(result.statusCode)
        call.respondText(result.body, ContentType.Application.Json, status)
    }

    private suspend fun handleStreamResponse(call: ApplicationCall, route: RouteResult, body: String) {
        call.response.cacheControl(CacheControl.NoCache(null))
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            val channel = Channel<String>(Channel.BUFFERED)
            var streamDone = false
            var streamError: String? = null

            apiForwarder.forwardChatCompletionStream(
                route = route,
                requestBody = body,
                onEvent = { data -> channel.trySend(data) },
                onComplete = { streamDone = true },
                onError = { error -> streamError = error; streamDone = true }
            )

            while (!streamDone || !channel.isEmpty) {
                val data = channel.tryReceive().getOrNull()
                if (data != null) {
                    write("$data\n\n")
                    flush()
                } else if (!streamDone) {
                    kotlinx.coroutines.delay(50)
                }
            }

            if (streamError != null) {
                write("data: [DONE]\n\n")
                flush()
            }
        }
    }

    private suspend fun checkAuth(call: ApplicationCall): Boolean {
        if (proxyApiKey == "sk-local-proxy-key") return true
        val auth = call.request.header("Authorization") ?: ""
        val token = auth.removePrefix("Bearer ").trim()
        if (token != proxyApiKey) {
            call.respondText(
                "{\"error\":\"unauthorized\"}",
                ContentType.Application.Json,
                HttpStatusCode.Unauthorized
            )
            return false
        }
        return true
    }
}
