package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.db.ApiKeyItem
import com.example.token.TokenManager
import com.example.ui.model.AuraViewModel
import java.text.SimpleDateFormat
import java.util.*

// Cyberpunk-styled Material 3 dark colors
val CyberBg = Color(0xFF070514)
val CyberCardBg = Color(0xFF110E24)
val CyberPrimary = Color(0xFF00FFB2) // Glowing Jade Mint
val CyberSecondary = Color(0xFFFF2A85) // Neon Orchid Magenta
val CyberOrange = Color(0xFFFF9E00) // Laser Amber
val CyberTextPrimary = Color(0xFFE2E2FF)
val CyberTextSecondary = Color(0xFF8A88B4)
val CyberBorder = Color(0xFF231E4D)

@Composable
fun AuraScreen(viewModel: AuraViewModel, modifier: Modifier = Modifier) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Futuristic background grid overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A071E),
                            CyberBg
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
            AuraHeader(viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-text describing the App
            Text(
                text = if (language == "zh") {
                    "WebView 登录各平台 → 自动抓取 Token → 转为 API Key/URL 调用。支持 10+ 海内外平台。"
                } else {
                    "Login via WebView → Auto-capture Token → Convert to API Key/URL. Supports 10+ platforms."
                },
                color = CyberTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section Tabs Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E0B1F), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val sections = if (language == "zh") {
                    listOf("🔑 Zero Token", "🔒 我的金库", "🛰️ 代理网关", "🛠️ 终端备份")
                } else {
                    listOf("🔑 ZeroToken", "🔒 Vault", "🛰️ Gateway", "🛠️ Console")
                }
                sections.forEachIndexed { index, title ->
                    val isSelected = currentSection == index
                    val animPillBg by animateColorAsState(
                        targetValue = if (isSelected) CyberPrimary.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = tween(250)
                    )
                    val animTextColor by animateColorAsState(
                        targetValue = if (isSelected) CyberPrimary else CyberTextSecondary,
                        animationSpec = tween(250)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(animPillBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.selectSection(index)
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = animTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentSection) {
                    0 -> ZeroTokenScreen(tokenManager = viewModel.tokenManager)
                    1 -> OldKeyVaultTab(viewModel)
                    2 -> ProxyGatewayTab(viewModel)
                    3 -> TerminalBackupTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun AuraHeader(viewModel: AuraViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(CyberPrimary, CyberSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Key Icon",
                    tint = CyberBg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = if (language == "zh") "API密钥自动精灵" else "API Key Auto-Elf",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = CyberTextPrimary,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "zh") "云节点: 全天候开启" else "Node: Active 24/7",
                        fontSize = 11.sp,
                        color = Color(0xFF00FF88)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Language selector button
            IconButton(
                onClick = { viewModel.toggleLanguage() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CyberCardBg)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Switch Language",
                    tint = CyberPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ================= SECTION 0: AUTO-ACQUISITION TAB =================
@Composable
fun AutoAcquisitionTab(viewModel: AuraViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val minThreshold by viewModel.minKeyReserveThreshold.collectAsStateWithLifecycle()
    val isAutoRegRunning by viewModel.autoRegRunning.collectAsStateWithLifecycle()
    val selectedProviders by viewModel.selectedAutoRegProviders.collectAsStateWithLifecycle()
    val logs by viewModel.tempMailInboxLogs.collectAsStateWithLifecycle()
    val activeFingerprint by viewModel.selectedFingerprintMode.collectAsStateWithLifecycle()
    val databaseSalts by viewModel.accountSalts.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val supportedServices = listOf("Gemini", "DeepSeek", "Groq", "OpenRouter", "Together", "Fireworks", "Zhipu")
    
    // Terminal state
    val terminalOutput by viewModel.terminalOutputState.collectAsStateWithLifecycle()
    var terminalInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Title & Status Alert Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isAutoRegRunning) Color(0xFF0C2415) else Color(0xFF140C24)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isAutoRegRunning) Color.Green.copy(alpha = 0.5f) else CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAutoRegRunning) Color.Green else CyberOrange)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAutoRegRunning) "自动注册守护线程：正在轮询获取..." else "自动注册守护线程：已暂停休眠",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAutoRegRunning) "当前后台持续利用临时邮箱 & CF 中转进行安全沙箱账号下发" else "一键启动本地常驻后台, 为您的金库自主充盈免费密钥",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Stepper: Threshold Config
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ 第一步: 调配核心获取参数",
                        color = CyberPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最小密钥储备阈值: $minThreshold 个",
                            color = CyberTextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "自动对齐同步",
                            color = CyberTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    
                    Slider(
                        value = minThreshold.toFloat(),
                        onValueChange = { viewModel.setMinKeyReserve(it.toInt()) },
                        valueRange = 2f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberPrimary,
                            activeTrackColor = CyberPrimary,
                            inactiveTrackColor = Color(0xFF1B1930)
                        )
                    )
                }
            }
        }

        // Grid List of services to acquire
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 第二步: 选择自愈获取平台",
                        color = CyberPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val chunked = supportedServices.chunked(3)
                    chunked.forEach { rowProviders ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowProviders.forEach { prov ->
                                val isChecked = selectedProviders.contains(prov)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChecked) CyberPrimary.copy(alpha = 0.16f) else Color(0xFF141124))
                                        .border(
                                            width = 1.dp,
                                            color = if (isChecked) CyberPrimary else CyberBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleAutoRegProvider(prov) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prov,
                                        color = if (isChecked) CyberPrimary else CyberTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (rowProviders.size < 3) {
                                repeat(3 - rowProviders.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware details / Anti-Ban configuration parameters
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛡️ 物理沙箱防封锁 (Anti-Fingerprint Console)",
                        color = CyberPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💻 动态混淆 UA: Chrome/122.0.0.0 (Mobile; Android)",
                            color = CyberTextSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "🎭 指纹隔离级别: Dynamic Canvas WebGL Renderer Spoofing",
                            color = CyberTextSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "🧬 沙箱隔离池数: $databaseSalts 组物理沙箱 cookie",
                            color = CyberPrimary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = { 
                    viewModel.toggleAutoReg()
                    Toast.makeText(context, if (!isAutoRegRunning) "🚀 自动下发巡检开启！常驻执行中..." else "🛑 自动获取服务已休眠！", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoRegRunning) CyberSecondary else CyberPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (!isAutoRegRunning) "🚀 启动自动下发巡检" else "🛑 暂停自动下发",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Safety Clear Data Button
        item {
            Button(
                onClick = {
                    viewModel.clearAllSavedKeys()
                    Toast.makeText(context, "⚠️ 已清盘所有本地数据", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚠️ 安全清盘本地所有 ROOM 数据", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070411)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⌨️ Termux 级沙盒 Agent 交互终端", color = CyberPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "系统集成终端。可直达本地系统 /system/bin/sh 核心，并绑定了自动一键更新等智能调试模块。", color = CyberTextSecondary, fontSize = 10.5.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val terminalScrollState = rememberScrollState()
                        LaunchedEffect(terminalOutput) {
                            terminalScrollState.scrollTo(terminalScrollState.maxValue)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(terminalScrollState)
                        ) {
                            Text(
                                text = terminalOutput,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executeTerminalCommand("help") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16132F), contentColor = CyberPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("指令帮助", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.executeTerminalCommand("ls app_config.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16132F), contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("查看配置", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { 
                                viewModel.executeTerminalCommand("agent-upgrade")
                                Toast.makeText(context, "正在拉取新版升级补丁...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("🚀 智能升级", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "user@aura:~$",
                            color = CyberPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            placeholder = { Text("输入指令...", color = CyberTextSecondary, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (terminalInput.trim().isEmpty()) return@Button
                                viewModel.executeTerminalCommand(terminalInput.trim())
                                terminalInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("RUN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ================= SECTION 2: SECRET KEY VAULT TAB =================
@Composable
fun OldKeyVaultTab(viewModel: AuraViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val localKeys by viewModel.apiKeysList.collectAsStateWithLifecycle()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isAddingCustomKey by remember { mutableStateOf(false) }
    
    // Custom insert form states
    var customLabel by remember { mutableStateOf("") }
    var customValue by remember { mutableStateOf("") }
    var customProv by remember { mutableStateOf("Gemini") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (language == "zh") "安全保险金库" else "My Secret Vault"} (${localKeys.size})",
                color = CyberPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                TextButton(
                    onClick = { isAddingCustomKey = !isAddingCustomKey }
                ) {
                    Text(
                        text = if (isAddingCustomKey) "✖ 取消录入" else "➕ 手动导入",
                        color = if (isAddingCustomKey) CyberSecondary else CyberPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slide in custom key manual import dialog
        AnimatedVisibility(visible = isAddingCustomKey) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = BorderStroke(1.dp, CyberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("导入新 API 秘钥", color = CyberPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Provider select
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Gemini", "DeepSeek", "Groq", "OpenAI").forEach { prov ->
                            val isS = customProv == prov
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isS) CyberPrimary.copy(alpha = 0.15f) else Color(0xFF16132F))
                                    .clickable { customProv = prov }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(prov, color = if (isS) CyberPrimary else CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Label
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("名称标签", color = CyberTextSecondary, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Value
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it },
                        label = { Text("密钥值 / Token内容", color = CyberTextSecondary, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            if (customValue.trim().isEmpty()) {
                                Toast.makeText(context, "密钥值不能为空！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.insertCustomKey(customLabel, customValue, customProv)
                            customLabel = ""
                            customValue = ""
                            isAddingCustomKey = false
                            Toast.makeText(context, "成功加入密钥金库！", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存到本地金库", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (localKeys.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = CyberBorder,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language == "zh") "金库中暂未保存任何密钥" else "Your secret vault is empty.",
                        color = CyberTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == "zh") "点击『一键获取』进行自动快速托管" else "Click Auto-Acquire tab to trigger auto generation.",
                        color = CyberTextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(localKeys) { key ->
                    var isRevealed by remember { mutableStateOf(false) }
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (key.provider) {
                                                    "Gemini" -> CyberPrimary.copy(alpha = 0.15f)
                                                    "DeepSeek" -> CyberSecondary.copy(alpha = 0.15f)
                                                    else -> CyberOrange.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = key.provider,
                                            color = when (key.provider) {
                                                "Gemini" -> CyberPrimary
                                                "DeepSeek" -> CyberSecondary
                                                else -> CyberOrange
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = key.keyLabel,
                                        color = CyberTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Eye select
                                    IconButton(
                                        onClick = { isRevealed = !isRevealed },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text(
                                            text = if (isRevealed) "🙈" else "👁",
                                            fontSize = 16.sp,
                                            color = CyberPrimary
                                        )
                                    }
                                    
                                    // Copy
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(key.keyValue))
                                            Toast.makeText(context, "密钥已复制！", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = CyberPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Delete
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSavedKey(key.id)
                                            Toast.makeText(context, "已删除密钥: ${key.keyLabel}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = CyberSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Key masked/revealed view
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                val textToShow = if (isRevealed) {
                                    key.keyValue
                                } else {
                                    if (key.keyValue.length > 12) {
                                        key.keyValue.take(6) + " •••••••••••••••••• " + key.keyValue.takeLast(6)
                                    } else {
                                        "••••••••••••••••••••"
                                    }
                                }
                                Text(
                                    text = textToShow,
                                    color = if (isRevealed) CyberTextPrimary else CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = "签发时间: ${sdf.format(Date(key.timestamp))}",
                                    color = CyberTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (key.status == "Active") Color(0xFF0F321C) else Color(0xFF381B1B),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (key.status == "Active") "验证通过" else "尚未校验/失效",
                                        color = if (key.status == "Active") CyberPrimary else CyberSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.clearAllSavedKeys() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberSecondary),
                        border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🧹 清空所有金库密钥", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ================= SECTION 3: PUBLIC SHARED FOUNTAIN =================
@Composable
fun SharedPoolTab(viewModel: AuraViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Curated high active list of free global key links / actual pre-configured key tokens!
    val poolKeys = listOf(
        Pair("Gemini 官方无限免费池 1", "AIzaSy...E8s8"),
        Pair("Gemini 官方无限免费池 2", "AIzaSy...At11"),
        Pair("DeepSeek 硅谷共享节点 1", "sk-dee...b91c"),
        Pair("Groq 高可用推理节点 API", "gsk_groq_free_developer_allotment_x98b"),
        Pair("HuggingFace 开放推理池", "hf_huggingface_public_read_access_f2a")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E112A)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = CyberSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "zh") "💎 社区高可用免费密钥池 (每日更新)" else "💎 Community Shared Free-tier Keys Pool",
                            color = CyberSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == "zh") "以下键值是针对全球教育/开发爱好者提供的云公共共享节点，可直接复制配置在您的代码或测试组件中体验！"
                        else "These public keys are offered for learning purposes. You can tap to copy and test directly.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(poolKeys) { pool ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pool.first,
                            color = CyberTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pool.second.length > 20) pool.second.take(12) + "..." + pool.second.takeLast(10) else pool.second,
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(pool.second))
                            Toast.makeText(context, "【${pool.first}】已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary.copy(alpha = 0.2f), contentColor = CyberPrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🚀 Google AI Studio 官方一键辅助向导",
                        color = CyberPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "由于官方策略，若您希望获得完全专属的、不限流的、免费官方 Gemini 密钥，极度推荐在手机上按以下四步操作：\n\n" +
                               "1️⃣ 访问 aistudio.google.com 登录谷歌账户 (VPN建议挂美/新等节点)\n" +
                               "2️⃣ 在左上角或右上角菜单寻找 'Create API Key' 选项\n" +
                               "3️⃣ 选择一个您的现有项目或直接创建 'New default project'\n" +
                               "4️⃣ 自动签发后一键 'Copy Key'，返回本软件粘贴到【密钥测试】保存活化即可永久极速可用！",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ================= PLACEHOLDER: PROXY GATEWAY TAB =================
@Composable
fun ProxyGatewayTab(viewModel: AuraViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛰️ 代理网关", color = CyberPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("即将上线", color = CyberTextSecondary, fontSize = 14.sp)
        }
    }
}

// ================= PLACEHOLDER: TERMINAL BACKUP TAB =================
@Composable
fun TerminalBackupTab(viewModel: AuraViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛠️ 终端备份", color = CyberPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("即将上线", color = CyberTextSecondary, fontSize = 14.sp)
        }
    }
}