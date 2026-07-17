package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentStepLog
import com.example.data.db.TokenEntity
import com.example.data.health.TokenHealthChecker
import com.example.data.health.TokenHealthStatus
import com.example.data.repository.TokenRepository
import com.example.proxy.LocalProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tokens: List<TokenEntity> = emptyList(),
    val proxyRunning: Boolean = false,
    val proxyPort: Int = 8080,
    val isLoading: Boolean = true,
    val error: String? = null,
    val agentLogs: List<AgentStepLog> = emptyList(),
    val agentToolCount: Int = 0,
    val healthStatus: Map<Long, TokenHealthStatus> = emptyMap(),
    val isCheckingHealth: Boolean = false
)

class DashboardViewModel(
    private val tokenRepository: TokenRepository,
    private val proxyServer: LocalProxyServer,
    private val healthChecker: TokenHealthChecker?
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var statusPollJob: Job? = null

    init {
        viewModelScope.launch {
            tokenRepository.getAllTokens().collect { tokens ->
                _uiState.update { it.copy(tokens = tokens, isLoading = false) }
            }
        }
        viewModelScope.launch {
            healthChecker?.healthStatus?.collect { status ->
                _uiState.update { it.copy(healthStatus = status) }
            }
        }
        startProxyIfNeeded()
        healthChecker?.startPeriodicCheck()
    }

    private fun startProxyIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                proxyServer.start()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "代理启动失败: ${e.message}") }
            }
        }
        startStatusPolling()
    }

    private fun startStatusPolling() {
        statusPollJob?.cancel()
        statusPollJob = viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(
                        proxyRunning = proxyServer.isRunning(),
                        agentLogs = proxyServer.agentLogs.toList()
                    )
                }
                delay(2000)
            }
        }
    }

    fun toggleProxy() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (proxyServer.isRunning()) {
                    proxyServer.stop()
                    statusPollJob?.cancel()
                } else {
                    proxyServer.start()
                    startStatusPolling()
                }
                _uiState.update { it.copy(proxyRunning = proxyServer.isRunning(), error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }

    fun deleteToken(id: Long) {
        viewModelScope.launch { tokenRepository.deleteToken(id) }
    }

    fun clearAgentLogs() {
        proxyServer.agentLogs.clear()
        _uiState.update { it.copy(agentLogs = emptyList()) }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true) }
            healthChecker?.checkAllTokens()
            _uiState.update { it.copy(isCheckingHealth = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        statusPollJob?.cancel()
        healthChecker?.stop()
    }
}
