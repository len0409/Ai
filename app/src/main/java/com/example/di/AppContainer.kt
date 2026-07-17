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
    }

    val agentOrchestrator by lazy {
        AgentOrchestrator(toolRegistry, apiForwarder, maxIterations = 20).apply {
            onConfirmRequired = { msg -> true }
            onProgress = { /* can be observed via logs */ }
        }
    }

    val tokenHealthChecker by lazy {
        TokenHealthChecker(tokenRepository, apiForwarder)
    }

    val proxyServer: LocalProxyServer by lazy {
        LocalProxyServer(
            port = preferencesRepository.getProxyPort(),
            proxyApiKey = preferencesRepository.getProxyApiKey(),
            tokenRepository = tokenRepository,
            modelRouter = modelRouter,
            apiForwarder = apiForwarder,
            agentOrchestrator = agentOrchestrator
        )
    }
}
