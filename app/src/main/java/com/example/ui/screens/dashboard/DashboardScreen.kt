package com.example.ui.screens.dashboard

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AgentStepLog
import com.example.data.health.TokenHealthStatus
import com.example.ui.components.TokenCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onToggleProxy: () -> Unit,
    onDeleteToken: (Long) -> Unit,
    onClearLogs: () -> Unit,
    onCheckHealth: () -> Unit,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("控制台", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { ProxyControlCard(uiState, onToggleProxy, clipboard) }
                    item { AgentStatusCard(uiState, onClearLogs) }
                    item {
                        Column {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("已获取 Token (${uiState.tokens.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (uiState.tokens.isNotEmpty()) {
                                    val activeCount = uiState.tokens.count { it.status == "active" }
                                    val healthyCount = uiState.healthStatus.count { it.value.isHealthy }
                                    if (healthyCount > 0) {
                                        TokenStatBadge("健康: $healthyCount", AccentGreen)
                                    }
                                }
                            }
                            if (uiState.tokens.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = onCheckHealth,
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                                    enabled = !uiState.isCheckingHealth
                                ) {
                                    if (uiState.isCheckingHealth) {
                                        CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = AccentBlue)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text("检测 Token 有效性", color = AccentBlue, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (uiState.tokens.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VpnKey, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("暂无 Token", color = TextSecondary, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("返回平台列表登录获取", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    items(uiState.tokens, key = { it.id }) { token ->
                        val health = uiState.healthStatus[token.id]
                        TokenCard(
                            token = token,
                            healthStatus = health,
                            onCopy = { clipboard.setText(AnnotatedString(it)) },
                            onDelete = { onDeleteToken(token.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AgentStatusCard(uiState: DashboardUiState, onClearLogs: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agent 引擎", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (uiState.proxyRunning) {
                    Surface(color = AccentGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text("就绪", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "12 tools: shell, file_r/w/search, content_search, git, code_analyze, web_fetch, device_info, clipboard, todo, knowledge",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip("并行执行")
                FeatureChip("危险命令确认")
                FeatureChip("上下文压缩")
                FeatureChip("跨会话记忆")
            }

            if (uiState.agentLogs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("最近执行 (${uiState.agentLogs.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("清除", color = AccentRed.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }

                uiState.agentLogs.takeLast(5).reversed().forEach { log ->
                    AgentLogItem(log)
                }
            }
        }
    }
}

@Composable
private fun AgentLogItem(log: AgentStepLog) {
    val bgColor = if (log.isError) AccentRed.copy(alpha = 0.08f) else AccentGreen.copy(alpha = 0.08f)
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (log.isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    null,
                    tint = if (log.isError) AccentRed else AccentGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("${log.toolName}", color = if (log.isError) AccentRed else AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.weight(1f))
                Text("#${log.iteration}", color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Text(
                log.arguments.take(100),
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ProxyControlCard(
    uiState: DashboardUiState,
    onToggle: () -> Unit,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (uiState.proxyRunning) AccentGreen.copy(alpha = 0.4f) else BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                        .background(if (uiState.proxyRunning) AccentGreen else AccentOrange)
                )
                Spacer(Modifier.width(10.dp))
                Text("本地代理服务", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (uiState.proxyRunning) "● 运行中" else "○ 已停止",
                    color = if (uiState.proxyRunning) AccentGreen else TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))
                Text("127.0.0.1:${uiState.proxyPort}", color = AccentBlue, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "API: http://127.0.0.1:${uiState.proxyPort}/v1",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.proxyRunning) AccentRed else AccentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (uiState.proxyRunning) "停止服务" else "启动服务", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString("http://127.0.0.1:${uiState.proxyPort}/v1")) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("复制 URL", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TokenStatBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        color = AccentBlue.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text, color = AccentBlue, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}
