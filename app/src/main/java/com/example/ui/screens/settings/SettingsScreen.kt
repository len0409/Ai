package com.example.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdatePort: (String) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) snackbarHostState.showSnackbar("设置已保存")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text("保存", color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SettingsSection("代理服务器") {
                OutlinedTextField(
                    value = uiState.proxyPort,
                    onValueChange = onUpdatePort,
                    label = { Text("端口号") },
                    placeholder = { Text("8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = darkTextFieldColors()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.proxyApiKey,
                    onValueChange = onUpdateApiKey,
                    label = { Text("API 密钥 (可选)") },
                    placeholder = { Text("sk-local-proxy-key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = darkTextFieldColors()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "设置后将要求客户端请求携带此密钥",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            SettingsSection("启动行为") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("自动启动代理", color = TextPrimary, fontSize = 14.sp)
                        Text("打开控制台时自动启动代理服务", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.autoStartProxy,
                        onCheckedChange = onUpdateAutoStart,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.3f))
                    )
                }
            }

            SettingsSection("关于") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("版本", color = TextPrimary, fontSize = 14.sp)
                    Text("1.0.0", color = TextSecondary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "AI Token Relay 是一个本地 AI 代理工具。\n通过 WebView 登录获取 Token，在本机建立统一的 OpenAI 兼容 API 端点，让各种 AI 客户端都能用上多个平台的模型。",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardBg,
    unfocusedContainerColor = CardBg,
    disabledContainerColor = CardBg,
    focusedBorderColor = AccentGreen,
    unfocusedBorderColor = BorderDefault,
    cursorColor = AccentGreen,
    focusedLabelColor = AccentGreen,
    unfocusedLabelColor = TextSecondary
)
