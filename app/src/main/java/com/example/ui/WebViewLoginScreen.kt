package com.example.ui

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
import com.example.data.db.AppDatabase
import com.example.data.db.TokenEntity
import com.example.platform.AiPlatform
import com.example.webview.LoginWebView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(platform: AiPlatform, database: AppDatabase, onBack: () -> Unit, onSuccess: () -> Unit) {
    var status by remember { mutableStateOf("正在加载 ${platform.name} 登录页...") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录 ${platform.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color = if (success) AccentGreen.copy(alpha = 0.1f) else if (error != null) AccentRed.copy(alpha = 0.1f) else Color(0xFF21262D), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (success) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Token 已获取！", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(error ?: status, color = if (error != null) AccentRed else TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                if (error == null && !success) {
                    LoginWebView(
                        platform = platform,
                        onTokenCaptured = { token ->
                            scope.launch {
                                try {
                                    database.tokenDao().insert(TokenEntity(platformId = platform.id, label = platform.name, tokenValue = token, tokenType = platform.tokenType.name))
                                    success = true
                                } catch (e: Exception) { error = "保存失败: ${e.message}" }
                            }
                        },
                        onPageLoaded = { status = "请登录您的 ${platform.name} 账号" }
                    )
                } else if (success) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Token 已保存", color = TextPrimary, fontSize = 16.sp)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onSuccess, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) { Text("返回", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}