package com.example.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ForwardResult(val success: Boolean, val body: String, val statusCode: Int = 200)

class ApiForwarder {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun forwardChatCompletion(apiBaseUrl: String, authHeader: String, requestBody: String): ForwardResult =
        withContext(Dispatchers.IO) {
            val url = "$apiBaseUrl/chat/completions"
            val body = requestBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url)
                .header("Authorization", authHeader).header("Content-Type", "application/json")
                .post(body).build()
            try {
                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: "{\"error\":\"empty\"}"
                ForwardResult(response.isSuccessful, respBody, response.code)
            } catch (e: Exception) {
                ForwardResult(false, "{\"error\":\"${e.message}\"}", 502)
            }
        }
}