package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.platforms.AiPlatform
import com.example.platforms.PlatformRegistry
import com.example.token.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Zero Token - 登录网页抓取 session token
 * 流程：选平台 → WebView 登录 → 自动抓取 cookie/token → 保存
 */

private val ZtBg = Color(0xFF070514)
private val ZtCardBg = Color(0xFF110E24)
private val ZtPrimary = Color(0xFF00FFB2)
private val ZtSecondary = Color(0xFFFF2A85)
private val ZtOrange = Color(0xFFFF9E00)
private val ZtTextPrimary = Color(0xFFE2E2FF)
private val ZtTextSecondary = Color(0xFF8A88B4)
private val ZtBorder = Color(0xFF231E4D)

@Composable
fun ZeroTokenScreen(tokenManager: TokenManager) {
    var selectedPlatform by remember { mutableStateOf<AiPlatform?>(null) }
    var showWebView by remember { mutableStateOf(false) }
    val savedTokens = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(Unit) {
        savedTokens.clear()
        savedTokens.addAll(tokenManager.getSavedPlatformIds())
    }
    
    if (showWebView && selectedPlatform != null) {
        ZeroTokenWebView(
            platform = selectedPlatform!!,
            tokenManager = tokenManager,
            onTokenCaptured = {
                showWebView = false
                selectedPlatform = null
                savedTokens.clear()
                savedTokens.addAll(tokenManager.getSavedPlatformIds())
            },
            onBack = {
                showWebView = false
                selectedPlatform = null
            }
        )
    } else {
        PlatformListScreen(
            platforms = PlatformRegistry.getAllPlatforms(),
            savedTokens = savedTokens,
            onPlatformClick = { platform ->
                selectedPlatform = platform
                showWebView = true
            },
            onDeleteToken = { platformId ->
                tokenManager.removeToken(platformId)
                savedTokens.clear()
                savedTokens.addAll(tokenManager.getSavedPlatformIds())
            }
        )
    }
}

@Composable
fun PlatformListScreen(
    platforms: List<AiPlatform>,
    savedTokens: List<String>,
    onPlatformClick: (AiPlatform) -> Unit,
    onDeleteToken: (String) -> Unit
) {
    val globalPlatforms = platforms.filter { it.country == "global" }
    val chinaPlatforms = platforms.filter { it.country == "china" }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ZtBg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZtCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ZtBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🔑 Zero Token", color = ZtPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("登录网页 → 自动抓取 Token → 用于 API 调用", color = ZtTextSecondary, fontSize = 11.sp)
                    Text("已保存 ${savedTokens.size} 个平台", color = ZtOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        item { Text("🌏 海外平台", color = ZtPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        items(globalPlatforms.size) { i ->
            PlatformCard(globalPlatforms[i], savedTokens.contains(globalPlatforms[i].id), { onPlatformClick(globalPlatforms[i]) }, { onDeleteToken(globalPlatforms[i].id) })
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)); Text("🇨🇳 国内平台", color = ZtPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        items(chinaPlatforms.size) { i ->
            PlatformCard(chinaPlatforms[i], savedTokens.contains(chinaPlatforms[i].id), { onPlatformClick(chinaPlatforms[i]) }, { onDeleteToken(chinaPlatforms[i].id) })
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun PlatformCard(platform: AiPlatform, hasToken: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (hasToken) Color(0xFF0C2415) else ZtCardBg),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (hasToken) ZtPrimary.copy(alpha = 0.5f) else ZtBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hasToken) ZtPrimary else ZtOrange))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(platform.name, color = ZtTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(if (hasToken) "✅ Token 已保存" else "点击登录获取", color = if (hasToken) ZtPrimary else ZtTextSecondary, fontSize = 10.sp)
            }
            if (hasToken) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = ZtSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroTokenWebView(
    platform: AiPlatform,
    tokenManager: TokenManager,
    onTokenCaptured: () -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("请登录 ${platform.name}") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // 检测登录成功并抓取 token
    LaunchedEffect(currentUrl) {
        if (currentUrl.isEmpty()) return@LaunchedEffect
        
        // 不在登录页 = 登录成功
        val isLoginPage = currentUrl.contains("login") || currentUrl.contains("sign") || currentUrl.contains("auth")
        
        if (!isLoginPage && currentUrl != platform.url && currentUrl != platform.url + "/") {
            delay(2000) // 等页面加载完
            
            statusText = "正在抓取 Token..."
            
            // 从 Cookie 抓取
            val cookies = CookieManager.getInstance().getCookie(currentUrl) ?: ""
            
            if (cookies.isNotEmpty()) {
                // 提取关键 cookie 作为 token
                val token = extractToken(cookies, platform.id)
                
                if (token.isNotEmpty()) {
                    tokenManager.saveToken(TokenManager.TokenData(
                        platformId = platform.id,
                        token = token,
                        tokenType = "cookie",
                        cookies = cookies
                    ))
                    statusText = "✅ Token 抓取成功！"
                    delay(800)
                    onTokenCaptured()
                    return@LaunchedEffect
                }
            }
            
            // 尝试从 localStorage 抓取
            webView?.evaluateJavascript(
                "(function() { try { return localStorage.getItem('token') || localStorage.getItem('access_token') || sessionStorage.getItem('token') || ''; } catch(e) { return ''; } })()"
            ) { result ->
                val localToken = result?.removeSurrounding("\"") ?: ""
                if (localToken.isNotEmpty() && localToken != "null") {
                    tokenManager.saveToken(TokenManager.TokenData(
                        platformId = platform.id,
                        token = localToken,
                        tokenType = "bearer",
                        cookies = cookies
                    ))
                    statusText = "✅ Token 抓取成功！"
                    coroutineScope.launch {
                        delay(800)
                        onTokenCaptured()
                    }
                } else {
                    statusText = "⚠️ 未检测到 Token，请点击手动抓取"
                }
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(ZtBg)) {
        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text("登录 ${platform.name}", color = ZtTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(statusText, color = if (statusText.contains("✅")) ZtPrimary else ZtOrange, fontSize = 10.sp)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = ZtTextPrimary)
                }
            },
            actions = {
                // 手动抓取按钮
                Button(
                    onClick = {
                        statusText = "正在抓取..."
                        val cookies = CookieManager.getInstance().getCookie(currentUrl) ?: ""
                        
                        webView?.evaluateJavascript(
                            "(function() { try { return localStorage.getItem('token') || localStorage.getItem('access_token') || sessionStorage.getItem('token') || ''; } catch(e) { return ''; } })()"
                        ) { result ->
                            val localToken = result?.removeSurrounding("\"") ?: ""
                            val finalToken = if (localToken.isNotEmpty() && localToken != "null") {
                                localToken
                            } else {
                                extractToken(cookies, platform.id)
                            }
                            
                            if (finalToken.isNotEmpty()) {
                                tokenManager.saveToken(TokenManager.TokenData(
                                    platformId = platform.id,
                                    token = finalToken,
                                    tokenType = if (localToken.isNotEmpty()) "bearer" else "cookie",
                                    cookies = cookies
                                ))
                                statusText = "✅ Token 抓取成功！"
                                coroutineScope.launch {
                                    delay(800)
                                    onTokenCaptured()
                                }
                            } else {
                                statusText = "❌ 未找到 Token，请确保已登录"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZtPrimary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("抓取", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ZtCardBg)
        )
        
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ZtPrimary)
        }
        
        // WebView - 启用缩放
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    
                    val wv = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(wv, true)
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            currentUrl = url ?: ""
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            currentUrl = url ?: ""
                        }
                    }
                    
                    webChromeClient = WebChromeClient()
                    loadUrl(platform.loginUrl)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 从 cookie 字符串中提取关键 token
private fun extractToken(cookies: String, platformId: String): String {
    if (cookies.isEmpty()) return ""
    
    val cookiePairs = cookies.split(";").map { it.trim() }
    
    // 根据平台提取特定 cookie
    val tokenKeys = when (platformId) {
        "deepseek" -> listOf("ds_chat_token", "token", "session_id")
        "chatgpt" -> listOf("__Secure-next-auth.session-token", "access_token")
        "claude" -> listOf("sessionKey", "session_key")
        "gemini" -> listOf("SID", "SSID", "HSID")
        "kimi" -> listOf("token", "access_token", "kimi_token")
        "zhipu" -> listOf("chatglm_token", "token")
        "tongyi" -> listOf("login_aliyunid_ticket", "token")
        else -> listOf("token", "access_token", "session", "session_id", "auth")
    }
    
    for (key in tokenKeys) {
        for (cookie in cookiePairs) {
            if (cookie.startsWith("$key=", ignoreCase = true)) {
                return cookie.substringAfter("=").trim()
            }
        }
    }
    
    // 兜底：返回最长的 cookie 值
    return cookiePairs.maxByOrNull { it.length }?.substringAfter("=")?.trim() ?: ""
}
