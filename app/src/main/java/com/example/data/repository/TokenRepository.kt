package com.example.data.repository

import com.example.data.crypto.TokenCrypto
import com.example.data.db.TokenDao
import com.example.data.db.TokenEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TokenRepository(private val tokenDao: TokenDao) {
    fun getAllTokens(): Flow<List<TokenEntity>> = tokenDao.getAll().map { tokens ->
        tokens.map { it.copy(tokenValue = TokenCrypto.decrypt(it.tokenValue) ?: it.tokenValue) }
    }

    suspend fun getActiveTokenForPlatform(platformId: String): TokenEntity? = withContext(Dispatchers.IO) {
        val token = tokenDao.getActiveTokenForPlatform(platformId) ?: return@withContext null
        token.copy(tokenValue = TokenCrypto.decrypt(token.tokenValue) ?: token.tokenValue)
    }

    suspend fun insertToken(platformId: String, label: String, tokenValue: String, tokenType: String) = withContext(Dispatchers.IO) {
        val encrypted = TokenCrypto.encrypt(tokenValue)
        tokenDao.insert(
            TokenEntity(
                platformId = platformId,
                label = label,
                tokenValue = encrypted,
                tokenType = tokenType
            )
        )
    }

    suspend fun deleteToken(id: Long) = tokenDao.deleteById(id)
    suspend fun updateLastUsed(id: Long, timestamp: Long) = tokenDao.updateLastUsed(id, timestamp)
    suspend fun updateStatus(id: Long, status: String) = tokenDao.updateStatus(id, status)

    suspend fun getActiveTokens(): List<TokenEntity> = withContext(Dispatchers.IO) {
        tokenDao.getActiveTokens().map {
            it.copy(tokenValue = TokenCrypto.decrypt(it.tokenValue) ?: it.tokenValue)
        }
    }

    fun getTokensByPlatform(platformId: String): Flow<List<TokenEntity>> = tokenDao.getByPlatform(platformId).map { tokens ->
        tokens.map { it.copy(tokenValue = TokenCrypto.decrypt(it.tokenValue) ?: it.tokenValue) }
    }
}
