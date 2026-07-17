package com.example.proxy

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ForwardResult(val success: Boolean, val body: String, val statusCode: Int = 200, val error: String? = null)

class ApiForwarder {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun dispose() { scope.cancel() }

    suspend fun forwardChatCompletion(route: RouteResult, requestBody: String, maxRetries: Int = 2): ForwardResult {
        var lastResult: ForwardResult? = null
        repeat(maxRetries) { attempt ->
            val result = doForward(route, requestBody)
            if (result.success) return result
            lastResult = result
            if (attempt < maxRetries - 1) {
                delay(500L * (attempt + 1))
            }
        }
        return lastResult ?: ForwardResult(false, "{}", 502, "All retries exhausted")
    }

    private suspend fun doForward(route: RouteResult, requestBody: String): ForwardResult =
        withContext(Dispatchers.IO) {
            val url = "${route.apiBaseUrl}/chat/completions"
            val body = requestBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder().url(url)
                .header("Content-Type", "application/json")
                .post(body)
            applyAuth(requestBuilder, route)
            try {
                val response = client.newCall(requestBuilder.build()).execute()
                val respBody = response.body?.string() ?: "{\"error\":\"empty response\"}"
                ForwardResult(response.isSuccessful, respBody, response.code)
            } catch (e: IOException) {
                ForwardResult(false, "{\"error\":\"${e.message}\"}", 502, e.message)
            } catch (e: Exception) {
                ForwardResult(false, "{\"error\":\"internal: ${e.message}\"}", 500, e.message)
            }
        }

    suspend fun forwardChatCompletionStream(
        route: RouteResult,
        requestBody: String,
        onEvent: suspend (String) -> Unit,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url("${route.apiBaseUrl}/chat/completions")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
        applyAuth(requestBuilder, route)

        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(requestBuilder.build())
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    scope.launch { onError(e.message ?: "stream error") }
                    continuation.resumeWith(Result.success(Unit))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        scope.launch {
                            val respBody = response.body?.string() ?: ""
                            onError("upstream error ${response.code}: $respBody")
                        }
                        continuation.resumeWith(Result.success(Unit))
                        return
                    }
                    val source = response.body?.source() ?: run {
                        scope.launch { onError("empty stream") }
                        continuation.resumeWith(Result.success(Unit))
                        return
                    }
                    scope.launch {
                        try {
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                if (line.isEmpty()) continue
                                onEvent(line)
                            }
                            onComplete()
                        } catch (e: IOException) {
                            onError(e.message ?: "stream read error")
                        } finally {
                            response.close()
                        }
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
            })
        }
    }

    private fun applyAuth(builder: Request.Builder, route: RouteResult) {
        when (route.authType) {
            "ApiKey" -> builder.header("Authorization", "Bearer ${route.authHeader}")
            "Cookie" -> builder.header("Cookie", route.authHeader)
            else -> builder.header("Authorization", route.authHeader)
        }
    }

    suspend fun checkHealth(apiBaseUrl: String, authHeader: String, authType: String = "Bearer"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val builder = Request.Builder().url("$apiBaseUrl/models").get()
                when (authType) {
                    "ApiKey" -> builder.header("Authorization", "Bearer $authHeader")
                    "Cookie" -> builder.header("Cookie", authHeader)
                    else -> builder.header("Authorization", authHeader)
                }
                val response = client.newCall(builder.build()).execute()
                response.close()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
}
