package com.example.service

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TempMailClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun createAccount(): MailAccount? {
        return try {
            val id = (10000..99999).random().toString()
            val domain = listOf("1secmail.com", "1secmail.org", "1secmail.net").random()
            val login = "user${System.currentTimeMillis()}"
            val email = "$login@$domain"
            MailAccount(email = email, mailId = id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun waitForVerificationCode(account: MailAccount, timeoutMs: Long): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val domain = account.email.substringAfter("@")
                val login = account.email.substringBefore("@")
                val request = Request.Builder()
                    .url("https://www.1secmail.com/api/v1/?action=getMessages&login=$login&domain=$domain")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "[]"
                val messages = JSONArray(body)
                if (messages.length() > 0) {
                    val msg = messages.getJSONObject(0)
                    val msgId = msg.getInt("id")
                    val detailRequest = Request.Builder()
                        .url("https://www.1secmail.com/api/v1/?action=readMessage&login=$login&domain=$domain&id=$msgId")
                        .get()
                        .build()
                    val detailResponse = client.newCall(detailRequest).execute()
                    val detailBody = detailResponse.body?.string() ?: "{}"
                    val detail = JSONObject(detailBody)
                    val textBody = detail.optString("textBody", "")
                    val codeMatch = Regex("""\b(\d{4,8})\b""").find(textBody)
                    if (codeMatch != null) return codeMatch.groupValues[1]
                    val subject = detail.optString("subject", "")
                    val subjectMatch = Regex("""\b(\d{4,8})\b""").find(subject)
                    if (subjectMatch != null) return subjectMatch.groupValues[1]
                }
            } catch (_: Exception) { }
            delay(3000)
        }
        return null
    }
}