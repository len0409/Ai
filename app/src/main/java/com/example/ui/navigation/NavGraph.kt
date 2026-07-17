package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.di.AppContainer
import com.example.platform.PlatformRegistry
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.login.LoginViewModel
import com.example.ui.screens.login.WebViewLoginScreen
import com.example.ui.screens.platforms.PlatformListScreen
import com.example.ui.screens.platforms.PlatformListViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.AiRelayTheme

object NavRoutes {
    const val PLATFORMS = "platforms"
    const val LOGIN = "login/{platformId}"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"

    fun loginRoute(platformId: String) = "login/$platformId"
}

@Composable
fun AppNavGraph(container: AppContainer) {
    AiRelayTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = NavRoutes.PLATFORMS) {
            composable(NavRoutes.PLATFORMS) {
                val vm: PlatformListViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.NewInstanceFactory() {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return PlatformListViewModel(container.tokenRepository) as T
                        }
                    }
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                PlatformListScreen(
                    uiState = uiState,
                    onLogin = {
                        vm.selectPlatform(it)
                        navController.navigate(NavRoutes.loginRoute(it.id))
                    },
                    onOpenDashboard = { navController.navigate(NavRoutes.DASHBOARD) },
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }

            composable(
                NavRoutes.LOGIN,
                arguments = listOf(navArgument("platformId") { type = NavType.StringType })
            ) { backStackEntry ->
                val platformId = backStackEntry.arguments?.getString("platformId") ?: return@composable
                val platform = PlatformRegistry.getById(platformId) ?: return@composable

                val vm: LoginViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.NewInstanceFactory() {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LoginViewModel(container.tokenRepository) as T
                        }
                    }
                )
                LaunchedEffect(platformId) { vm.initPlatform(platform) }
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                WebViewLoginScreen(
                    uiState = uiState,
                    onTokenCaptured = { vm.onTokenCaptured(it) },
                    onPageLoaded = { vm.onPageLoaded() },
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack(NavRoutes.PLATFORMS, false) }
                )
            }

            composable(NavRoutes.DASHBOARD) {
                val vm: DashboardViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.NewInstanceFactory() {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return DashboardViewModel(container.tokenRepository, container.proxyServer, container.tokenHealthChecker) as T
                        }
                    }
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = uiState,
                    onToggleProxy = { vm.toggleProxy() },
                    onDeleteToken = { vm.deleteToken(it) },
                    onClearLogs = { vm.clearAgentLogs() },
                    onCheckHealth = { vm.checkHealth() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.NewInstanceFactory() {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SettingsViewModel(container.preferencesRepository) as T
                        }
                    }
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onUpdatePort = { vm.updatePort(it) },
                    onUpdateApiKey = { vm.updateApiKey(it) },
                    onUpdateAutoStart = { vm.updateAutoStart(it) },
                    onSave = { vm.saveSettings() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
