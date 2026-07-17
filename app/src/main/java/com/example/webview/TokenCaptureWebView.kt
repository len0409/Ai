package com.example.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.platform.AiPlatform
import com.example.platform.TokenType

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    platform: AiPlatform,
    onTokenCaptured: (String) -> Unit,
    onPageLoaded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var didCapture by remember { mutableStateOf(false) }

    DisposableEffect(platform.id) {
        didCapture = false
        onDispose { }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        didCapture = false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (didCapture || view == null || url == null) return
                        onPageLoaded()

                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (!cookies.isNullOrBlank()) {
                            val token = extractTokenFromCookies(cookies, platform)
                            if (token != null) {
                                didCapture = true
                                onTokenCaptured(token)
                                return
                            }
                            if (platform.tokenType == TokenType.COOKIE) {
                                didCapture = true
                                onTokenCaptured(cookies)
                                return
                            }
                        }

                        val jsCode = getExtractJs(platform) ?: return
                        view.evaluateJavascript(jsCode) { result ->
                            if (result != null && result != "null" && result != "\"\"" && result != "undefined" && !didCapture) {
                                val token = result.trim('"').trim()
                                if (token.isNotBlank()) {
                                    didCapture = true
                                    onTokenCaptured(token)
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

private fun getExtractJs(platform: AiPlatform): String? {
    return when (platform.id) {
        "deepseek" -> """
            (function() {
                try { var t = localStorage.getItem('userToken'); if (t) return t; } catch(e) {}
                try { var u = localStorage.getItem('userInfo'); if (u) { var p = JSON.parse(u); if (p.token) return p.token; } } catch(e) {}
                return null;
            })()
        """.trimIndent()

        "openai" -> """
            (function() {
                try { var t = localStorage.getItem('oai/app-session-token'); if (t) return t; } catch(e) {}
                try { var t = sessionStorage.getItem('oai/app-token'); if (t) return t; } catch(e) {}
                return null;
            })()
        """.trimIndent()

        "claude" -> """
            (function() {
                try { var t = localStorage.getItem('lastActiveOrganization'); if (t) return t; } catch(e) {}
                try { var t = sessionStorage.getItem('auth_token'); if (t) return t; } catch(e) {}
                return null;
            })()
        """.trimIndent()

        "gemini" -> """
            (function() {
                try { for (var i=0;i<localStorage.length;i++) { var k=localStorage.key(i); if (k.indexOf('firebase')!==-1||k.indexOf('google')!==-1) { var v=localStorage.getItem(k); try { var o=JSON.parse(v); if (o.stsTokenManager&&o.stsTokenManager.accessToken) return o.stsTokenManager.accessToken; } catch(e) {} } } } catch(e) {}
                return null;
            })()
        """.trimIndent()

        "kimi" -> """
            (function() {
                try { var t = localStorage.getItem('refresh_token'); if (t) return t; } catch(e) {}
                try { var t = localStorage.getItem('access_token'); if (t) return t; } catch(e) {}
                try { var t = sessionStorage.getItem('token'); if (t) return t; } catch(e) {}
                return null;
            })()
        """.trimIndent()

        else -> null
    }
}

private fun extractTokenFromCookies(cookieString: String, platform: AiPlatform): String? {
    val pairs = cookieString.split(";").map { it.trim() }

    val patterns = when (platform.id) {
        "deepseek" -> listOf("userToken", "chat_token", "auth_token")
        "openai" -> listOf("__Secure-next-auth.session-token", "oai/app-session-token")
        "claude" -> listOf("sessionKey", "claude_session", "session")
        "gemini" -> listOf("__Secure-1PSID", "SID", "__Secure-3PSID")
        "groq" -> listOf("groq-session", "session")
        "kimi" -> listOf("kimi_token", "refresh_token", "access_token")
        else -> listOf("token", "auth", "session", "access_token")
    }

    for (pattern in patterns) {
        val match = pairs.firstOrNull { it.startsWith("$pattern=", true) }
        if (match != null) {
            val value = match.substringAfter("=").removePrefix("Bearer ").trim()
            if (value.isNotBlank()) return value
        }
    }
    return null
}
