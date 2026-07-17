package com.example.di

import android.content.Context
import com.example.agent.AgentOrchestrator
import com.example.agent.ToolRegistry
import com.example.agent.tools.*
import com.example.data.db.AppDatabase
import com.example.data.health.TokenHealthChecker
import com.example.data.repository.TokenRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.proxy.ApiForwarder
import com.example.proxy.LocalProxyServer
import com.example.proxy.ModelRouter

class AppContainer(private val context: Context) {
    val database by lazy { AppDatabase.getInstance(context) }
    val preferencesRepository by lazy { UserPreferencesRepository(context) }
    val tokenRepository by lazy { TokenRepository(database.tokenDao()) }
    val modelRouter by lazy { ModelRouter(tokenRepository) }
    val apiForwarder by lazy { ApiForwarder() }

    val toolRegistry by lazy {
        try {
            ToolRegistry().apply {
                register(ShellExecTool())
                register(FileReadTool())
                register(FileWriteTool())
                register(FileSearchTool())
                register(ContentSearchTool())
                register(WebFetchTool())
                register(DeviceInfoTool())
                register(ClipboardTool(context))
                register(TodoListTool())
                register(GitTool())
                register(CodeAnalysisTool())
                register(KnowledgeMemoryTool())
            }
        } catch (e: Exception) {
            android.util.Log.e("AppContainer", "toolRegistry init failed", e)
            ToolRegistry() // fallback to empty registry
        }
    }

    val agentOrchestrator by lazy {
        try {
            AgentOrchestrator(toolRegistry, apiForwarder, maxIterations = 20).apply {
                onConfirmRequired = { msg -> true }
                onProgress = { /* can be observed via logs */ }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppContainer", "agentOrchestrator init failed", e)
            null
        }
    }

    val tokenHealthChecker by lazy {
        try {
            TokenHealthChecker(tokenRepository, apiForwarder)
        } catch (e: Exception) {
            android.util.Log.e("AppContainer", "tokenHealthChecker init failed", e)
            null
        }
    }

    val proxyServer: LocalProxyServer by lazy {
        LocalProxyServer(
            port = try { preferencesRepository.getProxyPort() } catch (_: Exception) { 8080 },
            proxyApiKey = try { preferencesRepository.getProxyApiKey() } catch (_: Exception) { "sk-local-proxy-key" },
            tokenRepository = tokenRepository,
            modelRouter = modelRouter,
            apiForwarder = apiForwarder,
            agentOrchestrator = agentOrchestrator
        )
    }
}
