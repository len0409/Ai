package com.example.proxy

import com.example.data.db.TokenDao
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
import kotlinx.serialization.json.*

class LocalProxyServer(
    private val port: Int,
    private val tokenDao: TokenDao,
    private val proxyApiKey: String = "sk-local-proxy-key"
) {
    @Volatile
    private var server: ApplicationEngine? = null

    fun start() {
        if (server != null) return
        server = embeddedServer(Netty, port = port) {
                module()
            }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun isRunning(): Boolean = server != null

    private fun Application.module() {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

        routing {
            get("/health") { call.respondText("OK", ContentType.Text.Plain) }

            get("/v1/models") {
                if (!checkAuth(call)) return@get
                val models = PlatformRegistry.getAllModels()
                val data = models.map { JsonObject(mapOf("id" to JsonPrimitive(it), "object" to JsonPrimitive("model"))) }
                call.respondText(JsonObject(mapOf("object" to JsonPrimitive("list"), "data" to JsonArray(data))).toString(), ContentType.Application.Json)
            }

            post("/v1/chat/completions") {
                if (!checkAuth(call)) return@post
                val body = call.receiveText()
                val json = Json.parseToJsonElement(body).jsonObject
                val modelId = json["model"]?.jsonPrimitive?.content ?: run {
                    call.respondText("{\"error\":\"model required\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@post
                }
                val router = ModelRouter(tokenDao)
                val route = router.route(modelId)
                if (route == null) {
                    call.respondText("{\"error\":\"no active token for '$modelId'\"}", ContentType.Application.Json, HttpStatusCode.ServiceUnavailable)
                    return@post
                }
                tokenDao.updateLastUsed(route.token.id, System.currentTimeMillis())
                val forwarder = ApiForwarder()
                val result = forwarder.forwardChatCompletion(route.apiBaseUrl, route.authHeader, body)
                val status = if (result.success) HttpStatusCode.OK else HttpStatusCode.fromValue(result.statusCode)
                call.respondText(result.body, ContentType.Application.Json, status)
            }
        }
    }

    private suspend fun checkAuth(call: ApplicationCall): Boolean {
        val auth = call.request.header("Authorization") ?: ""
        if (proxyApiKey == "sk-local-proxy-key") {
            // Allow all when using default key
            return true
        }
        return auth.removePrefix("Bearer ").trim() == proxyApiKey
    }
}