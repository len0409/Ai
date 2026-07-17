package com.example.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TokenRepository
import com.example.platform.AiPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val platform: AiPlatform? = null,
    val status: String = "",
    val error: String? = null,
    val success: Boolean = false
)

class LoginViewModel(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun initPlatform(platform: AiPlatform) {
        _uiState.update { it.copy(platform = platform, status = "正在加载 ${platform.name} 登录页...") }
    }

    fun onPageLoaded() {
        _uiState.update { state ->
            state.platform?.let { state.copy(status = "请登录您的 ${it.name} 账号") } ?: state
        }
    }

    fun onTokenCaptured(token: String) {
        viewModelScope.launch {
            val platform = _uiState.value.platform ?: return@launch
            try {
                tokenRepository.insertToken(
                    platformId = platform.id,
                    label = platform.name,
                    tokenValue = token,
                    tokenType = platform.tokenType.name
                )
                _uiState.update { it.copy(success = true, error = null, status = "Token 已获取") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }
}
