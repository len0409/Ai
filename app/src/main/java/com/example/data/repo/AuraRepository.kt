package com.example.data.repo

import com.example.data.db.ApiKeyDao
import com.example.data.db.ApiKeyItem
import com.example.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AuraRepository(private val database: AppDatabase) {

    private val apiKeyDao: ApiKeyDao = database.apiKeyDao()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val allKeys: Flow<List<ApiKeyItem>> = apiKeyDao.getAll()

    suspend fun insertKey(item: ApiKeyItem): Long {
        return apiKeyDao.insert(item)
    }

    suspend fun updateKey(item: ApiKeyItem) {
        apiKeyDao.updateStatus(item.id, item.status)
    }

    suspend fun deleteKeyById(id: Long) {
        apiKeyDao.deleteById(id)
    }

    suspend fun clearAllKeys() {
        apiKeyDao.clearAll()
    }

    suspend fun testApiKey(provider: String, key: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val url = when (provider.lowercase()) {
                "openai", "chatgpt" -> "https://api.openai.com/v1/models"
                "deepseek" -> "https://api.deepseek.com/v1/models"
                "groq" -> "https://api.groq.com/openai/v1/models"
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
                "cerebras" -> "https://api.cerebras.ai/v1/models"
                "mistral" -> "https://api.mistral.ai/v1/models"
                "claude" -> "https://api.anthropic.com/v1/models"
                "zhipu", "chatglm" -> "https://open.bigmodel.cn/api/paas/v4/models"
                else -> return@withContext Pair(false, "不支持的平台")
            }
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $key")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Pair(true, "✅ 密钥有效")
                } else {
                    Pair(false, "❌ 错误 ${response.code}: ${response.message}")
                }
            } catch (e: Exception) {
                Pair(false, "❌ 连接失败: ${e.message}")
            }
        }
    }
}