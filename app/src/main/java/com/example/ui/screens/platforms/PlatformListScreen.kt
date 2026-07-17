package com.example.ui.screens.platforms

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PlatformCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformListScreen(
    uiState: PlatformListUiState,
    onLogin: (PlatformWithStatus) -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Token Relay", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextPrimary)
                    }
                    IconButton(onClick = onOpenDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "控制台", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }
            uiState.platforms.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("暂无平台数据", color = TextSecondary, fontSize = 14.sp)
                }
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    Text("选择平台登录，自动获取 Token", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.platforms, key = { it.platform.id }) { item ->
                            PlatformCard(
                                platform = item.platform,
                                statusText = item.statusText,
                                isActive = item.isActive,
                                onClick = { onLogin(item) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    UsageGuide()
                }
            }
        }
    }
}

@Composable
private fun UsageGuide() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("使用方式", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "1. 点击平台图标进入 WebView 登录\n2. 登录成功后自动抓取 Token\n3. 进入控制台启动本地代理服务\n4. 客户端配置 http://127.0.0.1:8080/v1 即可使用",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
