package com.example.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.webview.LoginWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    uiState: LoginUiState,
    onTokenCaptured: (String) -> Unit,
    onPageLoaded: () -> Unit,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val platform = uiState.platform ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录 ${platform.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusBar(uiState = uiState)

            Box(Modifier.weight(1f)) {
                when {
                    uiState.error != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Warning, null, tint = AccentOrange, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(uiState.error, color = AccentRed, fontSize = 14.sp)
                            }
                        }
                    }
                    uiState.success -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Token 已保存", color = TextPrimary, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("平台: ${platform.name}", color = TextSecondary, fontSize = 13.sp)
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = onSuccess,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("返回平台列表", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        LoginWebView(
                            platform = platform,
                            onTokenCaptured = onTokenCaptured,
                            onPageLoaded = onPageLoaded,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(uiState: LoginUiState) {
    val bgColor = when {
        uiState.success -> AccentGreen.copy(alpha = 0.1f)
        uiState.error != null -> AccentRed.copy(alpha = 0.1f)
        else -> Color(0xFF21262D)
    }

    Surface(color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                uiState.success -> {
                    Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Token 已获取", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    if (uiState.status.isNotBlank()) {
                        Text(uiState.status, color = if (uiState.error != null) AccentRed else TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
