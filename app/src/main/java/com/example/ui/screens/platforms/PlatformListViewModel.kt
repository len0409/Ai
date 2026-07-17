package com.example.ui.screens.platforms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.TokenEntity
import com.example.data.repository.TokenRepository
import com.example.platform.AiPlatform
import com.example.platform.PlatformRegistry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlatformListUiState(
    val tokens: List<TokenEntity> = emptyList(),
    val platforms: List<PlatformWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPlatform: AiPlatform? = null
)

data class PlatformWithStatus(
    val platform: AiPlatform,
    val token: TokenEntity?
) {
    val statusText: String get() = when {
        token == null -> "未获取"
        token.status == "active" -> "已就绪"
        else -> "已失效"
    }
    val isActive: Boolean get() = token?.status == "active"
}

class PlatformListViewModel(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatformListUiState())
    val uiState: StateFlow<PlatformListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenRepository.getAllTokens().collect { tokens ->
                _uiState.update { state ->
                    state.copy(
                        tokens = tokens,
                        platforms = PlatformRegistry.platforms.map { platform ->
                            PlatformWithStatus(
                                platform = platform,
                                token = tokens.find { it.platformId == platform.id }
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectPlatform(platform: AiPlatform) {
        _uiState.update { it.copy(selectedPlatform = platform) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPlatform = null) }
    }
}
