package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val proxyPort: String = "8080",
    val proxyApiKey: String = "",
    val autoStartProxy: Boolean = false,
    val saved: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.proxyPort,
                preferencesRepository.proxyApiKey,
                preferencesRepository.autoStartProxy
            ) { port, key, auto ->
                SettingsUiState(
                    proxyPort = port.toString(),
                    proxyApiKey = key,
                    autoStartProxy = auto
                )
            }.collect { state ->
                _uiState.update { it.copy(proxyPort = state.proxyPort, proxyApiKey = state.proxyApiKey, autoStartProxy = state.autoStartProxy) }
            }
        }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(proxyPort = port, saved = false) }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(proxyApiKey = key, saved = false) }
    }

    fun updateAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStartProxy = enabled, saved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val port = state.proxyPort.toIntOrNull() ?: 8080
            preferencesRepository.setProxyPort(port)
            preferencesRepository.setProxyApiKey(state.proxyApiKey)
            preferencesRepository.setAutoStartProxy(state.autoStartProxy)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
