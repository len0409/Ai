package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.platform.AiPlatform
import com.example.proxy.LocalProxyServer
import com.example.ui.*
import kotlinx.coroutines.*

enum class Screen { PLATFORMS, LOGIN, DASHBOARD }

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getInstance(this) }
    private val proxyServer by lazy { LocalProxyServer(port = 8080, tokenDao = database.tokenDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val tokens by database.tokenDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())
            var currentScreen by remember { mutableStateOf(Screen.PLATFORMS) }
            var selectedPlatform by remember { mutableStateOf<AiPlatform?>(null) }

            when (currentScreen) {
                Screen.PLATFORMS -> PlatformListScreen(
                    tokens = tokens,
                    onLogin = { selectedPlatform = it; currentScreen = Screen.LOGIN },
                    onOpenDashboard = { currentScreen = Screen.DASHBOARD }
                )
                Screen.LOGIN -> selectedPlatform?.let { platform ->
                    WebViewLoginScreen(platform = platform, database = database, onBack = { currentScreen = Screen.PLATFORMS }, onSuccess = { currentScreen = Screen.PLATFORMS })
                }
                Screen.DASHBOARD -> DashboardScreen(tokens = tokens, proxyServer = proxyServer, onDeleteToken = { id -> CoroutineScope(Dispatchers.Main).launch { database.tokenDao().deleteById(id) } }, onBack = { currentScreen = Screen.PLATFORMS })
            }
        }
    }

    override fun onDestroy() { proxyServer.stop(); super.onDestroy() }
}