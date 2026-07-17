package com.example.data.health

import com.example.data.db.TokenEntity
import com.example.data.repository.TokenRepository
import com.example.platform.PlatformRegistry
import com.example.platform.TokenType
import com.example.proxy.ApiForwarder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TokenHealthStatus(
    val tokenId: Long,
    val platformId: String,
    val isHealthy: Boolean,
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val error: String? = null
)

class TokenHealthChecker(
    private val tokenRepository: TokenRepository,
    private val apiForwarder: ApiForwarder
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _healthStatus = MutableStateFlow<Map<Long, TokenHealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<Long, TokenHealthStatus>> = _healthStatus.asStateFlow()

    fun startPeriodicCheck(intervalMs: Long = 600_000L) {
        scope.launch {
            while (isActive) {
                checkAllTokens()
                delay(intervalMs)
            }
        }
    }

    suspend fun checkAllTokens() {
        val tokens = tokenRepository.getActiveTokens()
        for (token in tokens) {
            val status = checkToken(token)
            _healthStatus.value = _healthStatus.value + (token.id to status)
            if (!status.isHealthy) {
                tokenRepository.updateStatus(token.id, "expired")
            }
        }
    }

    suspend fun checkSingleToken(tokenId: Long) {
        val all = _healthStatus.value.toMutableMap()
        val tokens = tokenRepository.getActiveTokens()
        val token = tokens.find { it.id == tokenId } ?: return
        val status = checkToken(token)
        all[tokenId] = status
        _healthStatus.value = all
        if (!status.isHealthy) {
            tokenRepository.updateStatus(token.id, "expired")
        }
    }

    private suspend fun checkToken(token: TokenEntity): TokenHealthStatus {
        val platform = PlatformRegistry.getById(token.platformId)
        if (platform == null) {
            return TokenHealthStatus(token.id, token.platformId, false, error = "unknown platform")
        }

        val tokenTypeEnum = try { TokenType.valueOf(token.tokenType) } catch (_: Exception) { TokenType.BEARER }
        val authType = when (tokenTypeEnum) {
            TokenType.COOKIE -> "Cookie"
            TokenType.API_KEY -> "ApiKey"
            TokenType.BEARER -> "Bearer"
        }

        val authHeader = when (tokenTypeEnum) {
            TokenType.API_KEY -> token.tokenValue
            TokenType.COOKIE -> token.tokenValue
            TokenType.BEARER -> "Bearer ${token.tokenValue}"
        }

        val isHealthy = apiForwarder.checkHealth(platform.apiBaseUrl, authHeader, authType)
        return TokenHealthStatus(
            tokenId = token.id,
            platformId = token.platformId,
            isHealthy = isHealthy,
            error = if (isHealthy) null else "health check failed"
        )
    }

    fun stop() {
        scope.cancel()
    }
}
