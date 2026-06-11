package com.example.service

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SmsActivateServiceImpl(private val apiKey: String) : SmsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.sms-activate.org/stubs/handler_api.php"

    override suspend fun getBalance(): Double {
        return try {
            val request = Request.Builder()
                .url("$baseUrl?api_key=$apiKey&action=getBalance")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (body.startsWith("ACCESS_BALANCE:")) {
                body.removePrefix("ACCESS_BALANCE:").trim().toDoubleOrNull() ?: 0.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    override suspend fun waitForCode(orderId: String, timeoutMs: Long): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl?api_key=$apiKey&action=getStatus&id=$orderId")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (body.startsWith("STATUS_OK:")) {
                    return body.removePrefix("STATUS_OK:").trim()
                }
            } catch (_: Exception) { }
            delay(3000)
        }
        return null
    }
}