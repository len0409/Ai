package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_relay_settings")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val PROXY_API_KEY = stringPreferencesKey("proxy_api_key")
        val AUTO_START_PROXY = booleanPreferencesKey("auto_start_proxy")
    }

    val proxyPort: Flow<Int> = context.dataStore.data.map { it[Keys.PROXY_PORT] ?: 8080 }
    val proxyApiKey: Flow<String> = context.dataStore.data.map { it[Keys.PROXY_API_KEY] ?: "sk-local-proxy-key" }
    val autoStartProxy: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_START_PROXY] ?: false }

    fun getProxyPort(): Int = 8080
    fun getProxyApiKey(): String = "sk-local-proxy-key"

    suspend fun setProxyPort(port: Int) {
        context.dataStore.edit { it[Keys.PROXY_PORT] = port }
    }

    suspend fun setProxyApiKey(key: String) {
        context.dataStore.edit { it[Keys.PROXY_API_KEY] = key }
    }

    suspend fun setAutoStartProxy(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_PROXY] = enabled }
    }
}
