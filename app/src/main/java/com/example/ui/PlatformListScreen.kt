package com.example.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TokenEntity
import com.example.platform.AiPlatform
import com.example.platform.PlatformRegistry

val DarkBg = Color(0xFF0D1117)
val CardBg = Color(0xFF161B22)
val AccentGreen = Color(0xFF00C853)
val AccentOrange = Color(0xFFFF9100)
val AccentRed = Color(0xFFFF1744)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformListScreen(
    tokens: List<TokenEntity>,
    onLogin: (AiPlatform) -> Unit,
    onOpenDashboard: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Token Relay", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("选择平台登录，自动获取 Token", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(PlatformRegistry.platforms) { platform ->
                    val token = tokens.find { it.platformId == platform.id }
                    val statusText = when {
                        token == null -> "未获取"
                        token.status == "active" -> "已就绪"
                        else -> "已失效"
                    }
                    PlatformCard(platform = platform, statusText = statusText, isActive = token?.status == "active", onClick = { onLogin(platform) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("使用方式", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("1. 点击平台 → WebView 登录\n2. 登录后自动抓取 Token\n3. 进入控制台启动代理服务\n4. 客户端填 http://127.0.0.1:8080", color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun PlatformCard(platform: AiPlatform, statusText: String, isActive: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isActive) AccentGreen.copy(alpha = 0.4f) else Color(0xFF30363D)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(if (isActive) AccentGreen.copy(alpha = 0.15f) else Color(0xFF21262D)), contentAlignment = Alignment.Center) {
                Text(platform.name.first().toString(), color = if (isActive) AccentGreen else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(platform.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            val statusColor = when { isActive -> AccentGreen; statusText == "未获取" -> TextSecondary; else -> AccentRed }
            Text("● $statusText", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}