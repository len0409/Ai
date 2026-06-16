package com.example.proxy

import com.example.data.db.TokenDao
import com.example.data.db.TokenEntity
import com.example.platform.PlatformRegistry

data class RouteResult(
    val platformId: String,
    val apiBaseUrl: String,
    val authHeader: String,
    val token: TokenEntity
)

class ModelRouter(private val tokenDao: TokenDao) {
    suspend fun route(modelId: String): RouteResult? {
        val platform = PlatformRegistry.findPlatformByModel(modelId) ?: return null
        val token = tokenDao.getActiveTokenForPlatform(platform.id) ?: return null
        val authHeader = "Bearer ${token.tokenValue}"
        return RouteResult(platformId = platform.id, apiBaseUrl = platform.apiBaseUrl, authHeader = authHeader, token = token)
    }
}