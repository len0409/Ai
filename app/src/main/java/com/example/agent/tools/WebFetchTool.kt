package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class WebFetchTool : Tool {
    override val name = "web_fetch"
    override val description = "Fetch content from a URL. Returns the response body as text."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "url" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("The URL to fetch")
            )),
            "method" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("HTTP method: GET or POST (default: GET)")
            )),
            "headers" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Optional JSON object of headers")
            )),
            "body" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Optional request body for POST")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("url")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val urlStr = arguments["url"]?.jsonPrimitive?.content
            ?: return@withContext ToolResult("", "missing required parameter: url", true)
        val method = arguments["method"]?.jsonPrimitive?.content ?: "GET"
        val bodyStr = arguments["body"]?.jsonPrimitive?.content

        return@withContext try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.requestMethod = method
            conn.setRequestProperty("User-Agent", "AI-Relay-Agent/1.0")

            if (bodyStr != null && method == "POST") {
                conn.doOutput = true
                conn.outputStream.write(bodyStr.toByteArray())
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.readText()?.take(20000) ?: ""
            conn.disconnect()

            if (code !in 200..299) {
                ToolResult("", "HTTP $code: ${body.take(500)}", true)
            } else {
                ToolResult("", body)
            }
        } catch (e: Exception) {
            ToolResult("", "fetch error: ${e.message}", true)
        }
    }
}
