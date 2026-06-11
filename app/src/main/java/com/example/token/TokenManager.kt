package com.example.token

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Token 管理器 - 统一管理所有平台的 token
 */
class TokenManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_tokens", Context.MODE_PRIVATE)
    private val tokenCache = ConcurrentHashMap<String, TokenData>()
    
    data class TokenData(
        val platformId: String,
        val token: String,
        val tokenType: String,  // cookie, bearer, api_key
        val cookies: String = "",
        val userAgent: String = "",
        val capturedAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = 0  // 0 表示不过期
    ) {
        fun isExpired(): Boolean {
            if (expiresAt == 0L) return false
            return System.currentTimeMillis() > expiresAt
        }
        
        fun toJson(): String {
            return JSONObject().apply {
                put("platformId", platformId)
                put("token", token)
                put("tokenType", tokenType)
                put("cookies", cookies)
                put("userAgent", userAgent)
                put("capturedAt", capturedAt)
                put("expiresAt", expiresAt)
            }.toString()
        }
        
        companion object {
            fun fromJson(json: String): TokenData {
                val obj = JSONObject(json)
                return TokenData(
                    platformId = obj.getString("platformId"),
                    token = obj.getString("token"),
                    tokenType = obj.getString("tokenType"),
                    cookies = obj.optString("cookies", ""),
                    userAgent = obj.optString("userAgent", ""),
                    capturedAt = obj.optLong("capturedAt", 0),
                    expiresAt = obj.optLong("expiresAt", 0)
                )
            }
        }
    }
    
    init {
        loadAllTokens()
    }
    
    /** 保存 token */
    fun saveToken(tokenData: TokenData) {
        tokenCache[tokenData.platformId] = tokenData
        prefs.edit().putString(tokenData.platformId, tokenData.toJson()).apply()
    }
    
    /** 保存 token (简化版) */
    fun saveToken(platformId: String, token: String, tokenType: String) {
        val tokenData = TokenData(
            platformId = platformId,
            token = token,
            tokenType = tokenType
        )
        saveToken(tokenData)
    }
    
    /** 获取 token */
    fun getToken(platformId: String): TokenData? {
        val token = tokenCache[platformId]
        if (token != null && token.isExpired()) {
            removeToken(platformId)
            return null
        }
        return token
    }
    
    /** 删除 token */
    fun removeToken(platformId: String) {
        tokenCache.remove(platformId)
        prefs.edit().remove(platformId).apply()
    }
    
    /** 检查是否有有效的 token */
    fun hasValidToken(platformId: String): Boolean {
        val token = getToken(platformId)
        return token != null && !token.isExpired()
    }
    
    /** 获取所有已保存的 token */
    fun getAllTokens(): List<TokenData> {
        return tokenCache.values.filter { !it.isExpired() }
    }
    
    /** 获取所有平台 ID */
    fun getSavedPlatformIds(): List<String> {
        return tokenCache.keys.toList()
    }
    
    /** 清除所有 token */
    fun clearAll() {
        tokenCache.clear()
        prefs.edit().clear().apply()
    }
    
    /** 从 WebView 抓取 token */
    suspend fun captureTokenFromWebView(
        webView: WebView,
        platformId: String,
        tokenType: String
    ): TokenData? = withContext(Dispatchers.Main) {
        try {
            val url = webView.url ?: return@withContext null
            val cookies = CookieManager.getInstance().getCookie(url) ?: ""
            
            // 根据平台类型提取 token
            val token = when (platformId) {
                "deepseek" -> extractDeepSeekToken(webView)
                "chatgpt" -> extractChatGptToken(webView)
                "claude" -> extractClaudeToken(webView)
                "kimi" -> extractKimiToken(webView)
                "tongyi" -> extractTongyiToken(webView)
                else -> extractGenericToken(webView, platformId)
            }
            
            if (token.isNullOrEmpty()) {
                return@withContext null
            }
            
            val tokenData = TokenData(
                platformId = platformId,
                token = token,
                tokenType = tokenType,
                cookies = cookies,
                userAgent = webView.settings.userAgentString ?: "",
                capturedAt = System.currentTimeMillis()
            )
            
            saveToken(tokenData)
            tokenData
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /** 提取 DeepSeek token */
    private suspend fun extractDeepSeekToken(webView: WebView): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    // 尝试从 localStorage 获取 token
                    var token = localStorage.getItem('token') || 
                                localStorage.getItem('access_token') ||
                                localStorage.getItem('ds_chat_token');
                    if (token) return token;
                    
                    // 尝试从 cookie 获取
                    var cookies = document.cookie;
                    var match = cookies.match(/token=([^;]+)/);
                    if (match) return match[1];
                    
                    return null;
                })()
            """)
        }
    }
    
    /** 提取 ChatGPT token */
    private suspend fun extractChatGptToken(webView: WebView): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    // ChatGPT 使用 access_token
                    var token = localStorage.getItem('oai-did') ||
                                localStorage.getItem('oai-ssid');
                    if (token) return token;
                    
                    // 尝试从 __NEXT_DATA__ 获取
                    try {
                        var nextData = document.getElementById('__NEXT_DATA__');
                        if (nextData) {
                            var data = JSON.parse(nextData.textContent);
                            return data.props?.pageProps?.accessToken || null;
                        }
                    } catch(e) {}
                    
                    return null;
                })()
            """)
        }
    }
    
    /** 提取 Claude token */
    private suspend fun extractClaudeToken(webView: WebView): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    // Claude 使用 session cookie
                    var cookies = document.cookie;
                    var match = cookies.match(/sessionKey=([^;]+)/);
                    if (match) return match[1];
                    return null;
                })()
            """)
        }
    }
    
    /** 提取 Kimi token */
    private suspend fun extractKimiToken(webView: WebView): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    var token = localStorage.getItem('token') ||
                                localStorage.getItem('access_token');
                    if (token) return token;
                    
                    var cookies = document.cookie;
                    var match = cookies.match(/token=([^;]+)/);
                    if (match) return match[1];
                    
                    return null;
                })()
            """)
        }
    }
    
    /** 提取通义千问 token */
    private suspend fun extractTongyiToken(webView: WebView): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    var cookies = document.cookie;
                    var match = cookies.match(/login_aliyunid_token=([^;]+)/);
                    if (match) return match[1];
                    return null;
                })()
            """)
        }
    }
    
    /** 通用 token 提取 */
    private suspend fun extractGenericToken(webView: WebView, platformId: String): String? {
        return withContext(Dispatchers.Main) {
            webView.evaluateJavascriptAsync("""
                (function() {
                    // 尝试常见的 token 存储位置
                    var keys = ['token', 'access_token', 'auth_token', 'session_token', 'bearer'];
                    for (var i = 0; i < keys.length; i++) {
                        var val = localStorage.getItem(keys[i]);
                        if (val) return val;
                    }
                    
                    // 尝试 cookie
                    var cookies = document.cookie;
                    var match = cookies.match(/token=([^;]+)/);
                    if (match) return match[1];
                    
                    return null;
                })()
            """)
        }
    }
    
    /** 从 SharedPreferences 加载所有 token */
    private fun loadAllTokens() {
        val all = prefs.all
        for ((key, value) in all) {
            if (value is String) {
                try {
                    val tokenData = TokenData.fromJson(value)
                    if (!tokenData.isExpired()) {
                        tokenCache[key] = tokenData
                    }
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
        }
    }
}

/** WebView 扩展：异步执行 JavaScript */
suspend fun WebView.evaluateJavascriptAsync(script: String): String? {
    return suspendCancellableCoroutine { continuation ->
        evaluateJavascript(script) { result ->
            val value = result?.removeSurrounding("\"")
            continuation.resume(value.takeIf { it != "null" && !it.isNullOrEmpty() }) {}
        }
    }
}
