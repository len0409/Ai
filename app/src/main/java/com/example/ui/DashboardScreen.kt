package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TokenEntity
import com.example.proxy.LocalProxyServer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(tokens: List<TokenEntity>, proxyServer: LocalProxyServer, onDeleteToken: (Long) -> Unit, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val proxyRunning by remember { mutableStateOf(proxyServer.isRunning()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("控制台", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (proxyRunning) AccentGreen.copy(alpha = 0.4f) else Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (proxyRunning) AccentGreen else AccentOrange))
                            Spacer(Modifier.width(10.dp))
                            Text("本地代理服务", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(if (proxyRunning) "● 运行中 - 127.0.0.1:8080" else "○ 已停止", color = if (proxyRunning) AccentGreen else TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("API URL: http://127.0.0.1:8080/v1", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { if (proxyRunning) proxyServer.stop() else proxyServer.start() }, colors = ButtonDefaults.buttonColors(containerColor = if (proxyRunning) AccentRed else AccentGreen), shape = RoundedCornerShape(8.dp)) {
                                Text(if (proxyRunning) "停止服务" else "启动服务", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString("http://127.0.0.1:8080/v1")) }, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, TextSecondary)) {
                                Text("复制 URL", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item { Text("已获取 Token (${tokens.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)) }
            items(tokens) { token ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if (token.status == "active") AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(token.platformId, color = if (token.status == "active") AccentGreen else AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(token.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(onClick = { clipboard.setText(AnnotatedString(token.tokenValue)) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = { onDeleteToken(token.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(token.tokenValue.take(30) + "..." + token.tokenValue.takeLast(10), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                        Text("获取时间: ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(token.createdAt))}", color = TextSecondary.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}