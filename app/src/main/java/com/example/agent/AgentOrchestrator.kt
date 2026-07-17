package com.example.agent

import com.example.proxy.ApiForwarder
import com.example.proxy.RouteResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class AgentOrchestrator(
    private val toolRegistry: ToolRegistry,
    private val apiForwarder: ApiForwarder,
    private val maxIterations: Int = 20
) {
    var onToolCall: ((String, String, String) -> Unit)? = null
    var onConfirmRequired: ((String) -> Boolean)? = null
    var onProgress: ((String) -> Unit)? = null

    private val resultCache = mutableMapOf<String, ToolResult>()
    private val memoryStore = mutableMapOf<String, String>()

    data class AgentContext(
        val route: RouteResult,
        val modelId: String,
        val messages: MutableList<JsonElement>,
        val log: MutableList<AgentStepLog>,
        val startTime: Long = System.currentTimeMillis()
    )

    suspend fun executeLoop(
        route: RouteResult,
        initialMessages: JsonArray,
        modelId: String
    ): AgentLoopResult = withContext(Dispatchers.IO) {
        val ctx = AgentContext(
            route = route,
            modelId = modelId,
            messages = initialMessages.toMutableList(),
            log = mutableListOf()
        )

        var iteration = 0
        while (iteration < maxIterations) {
            iteration++

            val requestBody = buildRequestBody(ctx.modelId, ctx.messages, toolRegistry.toToolsJson())
            val result = apiForwarder.forwardChatCompletion(ctx.route, requestBody, maxRetries = 2)

            if (!result.success) {
                val recovered = tryRecover(ctx, result, iteration)
                if (!recovered) {
                    return@withContext AgentLoopResult(false, "upstream error: ${result.body}", ctx.log, iteration)
                }
                continue
            }

            val responseJson = runCatching { Json.parseToJsonElement(result.body).jsonObject }.getOrNull()
            if (responseJson == null) {
                return@withContext AgentLoopResult(false, "invalid json from upstream: ${result.body.take(150)}", ctx.log, iteration)
            }
            val choices = responseJson["choices"]?.jsonArray
            val choice = choices?.firstOrNull()?.jsonObject
                ?: return@withContext AgentLoopResult(false, "no choices in response", ctx.log, iteration)
            val message = choice["message"]?.jsonObject
                ?: return@withContext AgentLoopResult(false, "no message in choice", ctx.log, iteration)

            val toolCalls = message["tool_calls"]?.jsonArray
            if (toolCalls == null || toolCalls.isEmpty()) {
                val content = message["content"]?.jsonPrimitive?.content ?: ""
                ctx.messages.add(message)
                val elapsed = (System.currentTimeMillis() - ctx.startTime) / 1000
                return@withContext AgentLoopResult(true, content, ctx.log, iteration, elapsed)
            }

            ctx.messages.add(message)

            if (toolCalls.size > 1 && areIndependent(toolCalls)) {
                executeParallelToolCalls(ctx, toolCalls)
            } else {
                for (tc in toolCalls) {
                    executeSingleToolCall(ctx, tc.jsonObject)
                }
            }

            val totalTokens = ctx.messages.sumOf { it.toString().length / 4 }
            if (totalTokens > 80000) {
                compressContext(ctx)
            }
        }

        AgentLoopResult(false, "reached max iterations ($maxIterations)", ctx.log, iteration)
    }

    private fun areIndependent(toolCalls: JsonArray): Boolean {
        val names = toolCalls.map { it.jsonObject["function"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "" }
        val independentTools = setOf("file_read", "file_search", "content_search", "device_info", "web_fetch")
        return names.all { it in independentTools } && names.toSet().size == names.size
    }

    private suspend fun executeParallelToolCalls(ctx: AgentContext, toolCalls: JsonArray) {
        onProgress?.invoke("并行执行 ${toolCalls.size} 个工具...")
        val results = coroutineScope {
            toolCalls.map { tc ->
                async(Dispatchers.IO) {
                    val tco = tc.jsonObject
                    val tcId = tco["id"]?.jsonPrimitive?.content ?: ""
                    val function = tco["function"]?.jsonObject
                    val name = function?.get("name")?.jsonPrimitive?.content ?: ""
                    val argsStr = function?.get("arguments")?.jsonPrimitive?.content ?: "{}"
                    val args = Json.parseToJsonElement(argsStr).jsonObject

                    onToolCall?.invoke(ctx.log.size.toString(), name, argsStr.take(150))

                    val tool = toolRegistry.get(name)
                    val result = if (tool != null) {
                        try { tool.execute(args) }
                        catch (e: Exception) { ToolResult(tcId, "error: ${e.message}", true) }
                    } else {
                        ToolResult(tcId, "unknown tool: $name", true)
                    }

                    Triple(tcId, name, result)
                }
            }.awaitAll()
        }

        for ((tcId, name, result) in results) {
            ctx.log.add(AgentStepLog(ctx.log.size + 1, name, "", result.isError, result.content.take(300)))
            ctx.messages.add(JsonObject(mapOf(
                "role" to JsonPrimitive("tool"),
                "tool_call_id" to JsonPrimitive(tcId),
                "name" to JsonPrimitive(name),
                "content" to JsonPrimitive(result.content)
            )))
        }
    }

    private suspend fun executeSingleToolCall(ctx: AgentContext, tc: JsonObject) {
        val tcId = tc["id"]?.jsonPrimitive?.content ?: return
        val function = tc["function"]?.jsonObject ?: return
        val name = function["name"]?.jsonPrimitive?.content ?: return
        val argsStr = function["arguments"]?.jsonPrimitive?.content ?: "{}"
        val args = runCatching { Json.parseToJsonElement(argsStr).jsonObject }.getOrElse { JsonObject(emptyMap()) }

        val cacheKey = "$name:${argsStr.take(80)}"
        resultCache[cacheKey]?.let { cached ->
            ctx.messages.add(JsonObject(mapOf(
                "role" to JsonPrimitive("tool"),
                "tool_call_id" to JsonPrimitive(tcId),
                "name" to JsonPrimitive(name),
                "content" to JsonPrimitive("[cached] ${cached.content}")
            )))
            return
        }

        if (isDangerousShell(name, argsStr)) {
            val confirmed = onConfirmRequired?.invoke("即将执行危险命令: $name $argsStr\n是否继续?") ?: true
            if (!confirmed) {
                ctx.messages.add(JsonObject(mapOf(
                    "role" to JsonPrimitive("tool"),
                    "tool_call_id" to JsonPrimitive(tcId),
                    "name" to JsonPrimitive(name),
                    "content" to JsonPrimitive("execution cancelled by user")
                )))
                return
            }
        }

        onToolCall?.invoke(ctx.log.size.toString(), name, argsStr.take(150))

        val tool = toolRegistry.get(name)
        val result = if (tool != null) {
            try { tool.execute(args) }
            catch (e: Exception) { ToolResult(tcId, "error: ${e.message}", true) }
        } else {
            ToolResult(tcId, "unknown tool: $name", true)
        }

        resultCache[cacheKey] = result
        ctx.log.add(AgentStepLog(ctx.log.size + 1, name, argsStr.take(200), result.isError, result.content.take(300)))

        ctx.messages.add(JsonObject(mapOf(
            "role" to JsonPrimitive("tool"),
            "tool_call_id" to JsonPrimitive(tcId),
            "name" to JsonPrimitive(name),
            "content" to JsonPrimitive(result.content)
        )))
    }

    private val dangerousPatterns = listOf(
        Regex("rm\\s+(-rf?|--recursive)"),
        Regex("chmod\\s+777"),
        Regex("mkfs\\."),
        Regex("dd\\s+if="),
        Regex(">\\s*/dev/"),
        Regex("shutdown|reboot|poweroff|halt"),
        Regex(":(){ :|:& };:"),
        Regex("chown\\s+-R"),
        Regex("iptables|nftables"),
        Regex("mount|umount"),
        Regex("parted|fdisk"),
        Regex("git\\s+push\\s+.*(--force|-f)")
    )

    private fun isDangerousShell(toolName: String, args: String): Boolean {
        if (toolName != "shell_exec") return false
        return dangerousPatterns.any { it.containsMatchIn(args) }
    }

    private suspend fun tryRecover(ctx: AgentContext, lastResult: com.example.proxy.ForwardResult, iteration: Int): Boolean {
        if (iteration <= 1) return false
        if (ctx.messages.size <= 4) return false

        val trimmed = ctx.messages.toMutableList()
        trimmed.removeAt(1)
        trimmed.removeAt(1)
        ctx.messages.clear()
        ctx.messages.addAll(trimmed)
        onProgress?.invoke("上下文压缩后重试...")
        return true
    }

    private fun compressContext(ctx: AgentContext) {
        val keep = ctx.messages.take(2).toMutableList()
        val tail = ctx.messages.takeLast(6)
        val summary = buildSummary(ctx.messages.subList(2, ctx.messages.size - 6))
        keep.add(JsonObject(mapOf(
            "role" to JsonPrimitive("system"),
            "content" to JsonPrimitive("[Previous conversation summary: $summary]")
        )))
        keep.addAll(tail)
        ctx.messages.clear()
        ctx.messages.addAll(keep)
    }

    private fun buildSummary(messages: List<JsonElement>): String {
        val toolCalls = messages.count { it is JsonObject && it["role"]?.jsonPrimitive?.content == "tool" }
        val fileOps = messages.filter {
            it is JsonObject && it["content"]?.jsonPrimitive?.content?.contains("successfully wrote") == true
        }
        return "$toolCalls tool calls, ${fileOps.size} file modifications performed"
    }

    fun remember(key: String, value: String) { memoryStore[key] = value }
    fun recall(key: String): String? = memoryStore[key]
    fun forget(key: String) { memoryStore.remove(key) }
    fun allMemories(): Map<String, String> = memoryStore.toMap()

    private fun buildRequestBody(model: String, messages: List<JsonElement>, tools: JsonArray): String {
        return JsonObject(mapOf(
            "model" to JsonPrimitive(model),
            "messages" to JsonArray(messages),
            "tools" to tools,
            "tool_choice" to JsonPrimitive("auto"),
            "temperature" to JsonPrimitive(0)
        )).toString()
    }
}

data class AgentLoopResult(
    val success: Boolean,
    val content: String,
    val logs: List<AgentStepLog>,
    val iterations: Int,
    val elapsedSeconds: Long = 0
)

data class AgentStepLog(
    val iteration: Int,
    val toolName: String,
    val arguments: String,
    val isError: Boolean,
    val resultSummary: String
)
