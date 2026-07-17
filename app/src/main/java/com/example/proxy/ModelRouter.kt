package com.example.proxy

import com.example.data.db.TokenEntity
import com.example.data.repository.TokenRepository
import com.example.platform.PlatformRegistry
import com.example.platform.TokenType

data class RouteResult(
    val platformId: String,
    val apiBaseUrl: String,
    val authHeader: String,
    val authType: String = "Bearer", // Bearer, ApiKey, Cookie
    val token: TokenEntity
)

class ModelRouter(private val tokenRepository: TokenRepository) {
    suspend fun route(modelId: String): RouteResult? {
        val platform = PlatformRegistry.findPlatformByModel(modelId) ?: return null
        val token = tokenRepository.getActiveTokenForPlatform(platform.id) ?: return null

        val tokenTypeEnum = try {
            TokenType.valueOf(token.tokenType)
        } catch (_: Exception) {
            TokenType.BEARER
        }

        val (authHeader, authType) = when (tokenTypeEnum) {
            TokenType.BEARER -> "Bearer ${token.tokenValue}" to "Bearer"
            TokenType.API_KEY -> token.tokenValue to "ApiKey"
            TokenType.COOKIE -> token.tokenValue to "Cookie"
        }

        return RouteResult(
            platformId = platform.id,
            apiBaseUrl = platform.apiBaseUrl,
            authHeader = authHeader,
            authType = authType,
            token = token
        )
    }
}
