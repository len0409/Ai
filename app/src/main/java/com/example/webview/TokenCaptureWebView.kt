package com.example.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.db.TokenEntity
import com.example.platform.AiPlatform
import com.example.platform.TokenType
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    platform: AiPlatform,
    onTokenCaptured: (String) -> Unit,
    onPageLoaded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var didCapture by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString = settings.userAgentString

                CookieManager.getInstance().setAcceptCookie(true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        didCapture = false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (didCapture || url == null) return
                        onPageLoaded()

                        // Try cookie capture
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (cookies != null && cookies.isNotBlank()) {
                            val token = extractTokenFromCookies(cookies, platform)
                            if (token != null) {
                                didCapture = true
                                onTokenCaptured(token)
                                return
                            }
                        }

                        // Try JS-based capture for platforms that store token in localStorage
                        if (platform.jsExtractCode != null) {
                            view?.evaluateJavascript(platform.jsExtractCode) { result ->
                                if (result != null && result != "null" && result != """" && !didCapture) {
                                    didCapture = true
                                    onTokenCaptured(result.trim('"'))
                                }
                            }
                        }
                    }
                }

                loadUrl(platform.loginUrl)
            }
        }
    )
}

private fun extractTokenFromCookies(cookieString: String, platform: AiPlatform): String? {
    val pairs = cookieString.split(";").map { it.trim() }
    return when (platform.id) {
        "deepseek", "openai" -> {
            pairs.firstOrNull { it.startsWith("Authorization=", true) }
                ?.substringAfter("=")?.removePrefix("Bearer ")
                ?: pairs.firstOrNull { it.startsWith("__Secure-next-auth.session-token=") }
                    ?.substringAfter("=")
        }
        "claude" -> {
            pairs.firstOrNull { it.startsWith("sessionKey=") }?.substringAfter("=")
        }
        "gemini" -> {
            pairs.firstOrNull { it.startsWith("__Secure-1PSID=") }?.substringAfter("=")
        }
        "kimi" -> {
            pairs.firstOrNull { it.startsWith("kimi_token=") }?.substringAfter("=")
        }
        else -> {
            pairs.firstOrNull { it.contains("token", true) }?.substringAfter("=")
        }
    }
}