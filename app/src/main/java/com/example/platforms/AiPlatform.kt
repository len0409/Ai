package com.example.platforms

/**
 * AI 平台配置 v2
 * 支持模型选择和 API Key/URL 转换
 */
data class AiPlatform(
    val id: String,
    val name: String,
    val url: String,
    val loginUrl: String,
    val chatUrl: String,
    val apiBaseUrl: String,
    val apiConsoleUrl: String, // API 控制台 URL，用于抓取 API Key
    val tokenType: TokenType,
    val tokenExtractMethod: TokenExtractMethod,
    val models: List<ModelInfo>,
    val country: String = "global",
    val isFree: Boolean = true,
    val supportsStreaming: Boolean = true
)

data class ModelInfo(
    val id: String,
    val name: String,
    val maxTokens: Int = 4096,
    val isDefault: Boolean = false
)

enum class TokenType {
    COOKIE,
    BEARER,
    API_KEY,
    SESSION
}

enum class TokenExtractMethod {
    COOKIE_INTERCEPT,
    LOCAL_STORAGE,
    JS_INJECTION,
    API_CALL,
    MANUAL_INPUT
}

/**
 * 平台注册表
 */
object PlatformRegistry {
    
    val platforms = listOf(
        // ========== DeepSeek ==========
        AiPlatform(
            id = "deepseek",
            name = "DeepSeek",
            url = "https://chat.deepseek.com",
            loginUrl = "https://chat.deepseek.com/sign_in",
            chatUrl = "https://chat.deepseek.com",
            apiBaseUrl = "https://api.deepseek.com/v1",
            apiConsoleUrl = "https://platform.deepseek.com/api_keys",
            tokenType = TokenType.BEARER,
            tokenExtractMethod = TokenExtractMethod.LOCAL_STORAGE,
            models = listOf(
                ModelInfo("deepseek-chat", "DeepSeek Chat", 4096, true),
                ModelInfo("deepseek-coder", "DeepSeek Coder", 4096),
                ModelInfo("deepseek-reasoner", "DeepSeek Reasoner", 4096)
            )
        ),
        
        // ========== ChatGPT ==========
        AiPlatform(
            id = "chatgpt",
            name = "ChatGPT",
            url = "https://chatgpt.com",
            loginUrl = "https://chatgpt.com/auth/login",
            chatUrl = "https://chatgpt.com",
            apiBaseUrl = "https://api.openai.com/v1",
            apiConsoleUrl = "https://platform.openai.com/api-keys",
            tokenType = TokenType.BEARER,
            tokenExtractMethod = TokenExtractMethod.LOCAL_STORAGE,
            models = listOf(
                ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", 4096, true),
                ModelInfo("gpt-4", "GPT-4", 8192),
                ModelInfo("gpt-4-turbo", "GPT-4 Turbo", 128000),
                ModelInfo("gpt-4o", "GPT-4o", 128000)
            )
        ),
        
        // ========== Claude ==========
        AiPlatform(
            id = "claude",
            name = "Claude",
            url = "https://claude.ai",
            loginUrl = "https://claude.ai/login",
            chatUrl = "https://claude.ai",
            apiBaseUrl = "https://api.anthropic.com/v1",
            apiConsoleUrl = "https://console.anthropic.com/settings/keys",
            tokenType = TokenType.SESSION,
            tokenExtractMethod = TokenExtractMethod.COOKIE_INTERCEPT,
            models = listOf(
                ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", 4096, true),
                ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet", 4096),
                ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", 4096),
                ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", 8192)
            )
        ),
        
        // ========== Gemini ==========
        AiPlatform(
            id = "gemini",
            name = "Gemini",
            url = "https://gemini.google.com",
            loginUrl = "https://gemini.google.com",
            chatUrl = "https://gemini.google.com",
            apiBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            apiConsoleUrl = "https://aistudio.google.com/app/apikey",
            tokenType = TokenType.API_KEY,
            tokenExtractMethod = TokenExtractMethod.MANUAL_INPUT,
            models = listOf(
                ModelInfo("gemini-pro", "Gemini Pro", 8192, true),
                ModelInfo("gemini-pro-vision", "Gemini Pro Vision", 4096),
                ModelInfo("gemini-ultra", "Gemini Ultra", 8192)
            )
        ),
        
        // ========== Groq ==========
        AiPlatform(
            id = "groq",
            name = "Groq",
            url = "https://console.groq.com",
            loginUrl = "https://console.groq.com/login",
            chatUrl = "https://console.groq.com/playground",
            apiBaseUrl = "https://api.groq.com/openai/v1",
            apiConsoleUrl = "https://console.groq.com/keys",
            tokenType = TokenType.API_KEY,
            tokenExtractMethod = TokenExtractMethod.MANUAL_INPUT,
            models = listOf(
                ModelInfo("llama3-8b-8192", "Llama 3 8B", 8192, true),
                ModelInfo("llama3-70b-8192", "Llama 3 70B", 8192),
                ModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B", 32768),
                ModelInfo("gemma-7b-it", "Gemma 7B", 8192)
            )
        ),
        
        // ========== Cerebras ==========
        AiPlatform(
            id = "cerebras",
            name = "Cerebras",
            url = "https://cloud.cerebras.ai",
            loginUrl = "https://cloud.cerebras.ai",
            chatUrl = "https://cloud.cerebras.ai",
            apiBaseUrl = "https://api.cerebras.ai/v1",
            apiConsoleUrl = "https://cloud.cerebras.ai/platform/api-keys",
            tokenType = TokenType.API_KEY,
            tokenExtractMethod = TokenExtractMethod.MANUAL_INPUT,
            models = listOf(
                ModelInfo("llama3.1-8b", "Llama 3.1 8B", 8192, true),
                ModelInfo("llama3.1-70b", "Llama 3.1 70B", 8192)
            )
        ),
        
        // ========== Mistral ==========
        AiPlatform(
            id = "mistral",
            name = "Mistral",
            url = "https://chat.mistral.ai",
            loginUrl = "https://chat.mistral.ai/login",
            chatUrl = "https://chat.mistral.ai",
            apiBaseUrl = "https://api.mistral.ai/v1",
            apiConsoleUrl = "https://console.mistral.ai/api-keys",
            tokenType = TokenType.SESSION,
            tokenExtractMethod = TokenExtractMethod.COOKIE_INTERCEPT,
            models = listOf(
                ModelInfo("mistral-tiny", "Mistral Tiny", 4096, true),
                ModelInfo("mistral-small", "Mistral Small", 4096),
                ModelInfo("mistral-medium", "Mistral Medium", 4096),
                ModelInfo("mistral-large", "Mistral Large", 4096)
            )
        ),
        
        // ========== Kimi ==========
        AiPlatform(
            id = "kimi",
            name = "Kimi",
            url = "https://kimi.moonshot.cn",
            loginUrl = "https://kimi.moonshot.cn/login",
            chatUrl = "https://kimi.moonshot.cn",
            apiBaseUrl = "https://api.moonshot.cn/v1",
            apiConsoleUrl = "https://platform.moonshot.cn/console/api-keys",
            tokenType = TokenType.SESSION,
            tokenExtractMethod = TokenExtractMethod.COOKIE_INTERCEPT,
            models = listOf(
                ModelInfo("moonshot-v1-8k", "Kimi 8K", 8192, true),
                ModelInfo("moonshot-v1-32k", "Kimi 32K", 32768),
                ModelInfo("moonshot-v1-128k", "Kimi 128K", 131072)
            ),
            country = "china"
        ),
        
        // ========== 通义千问 ==========
        AiPlatform(
            id = "tongyi",
            name = "通义千问",
            url = "https://tongyi.aliyun.com",
            loginUrl = "https://tongyi.aliyun.com/qianwen",
            chatUrl = "https://tongyi.aliyun.com/qianwen",
            apiBaseUrl = "https://dashscope.aliyuncs.com/api/v1",
            apiConsoleUrl = "https://dashscope.console.aliyun.com/apiKey",
            tokenType = TokenType.SESSION,
            tokenExtractMethod = TokenExtractMethod.COOKIE_INTERCEPT,
            models = listOf(
                ModelInfo("qwen-turbo", "通义千问 Turbo", 4096, true),
                ModelInfo("qwen-plus", "通义千问 Plus", 4096),
                ModelInfo("qwen-max", "通义千问 Max", 4096),
                ModelInfo("qwen-long", "通义千问 Long", 10000)
            ),
            country = "china"
        ),
        
        // ========== 智谱清言 ==========
        AiPlatform(
            id = "chatglm",
            name = "智谱清言",
            url = "https://chatglm.cn",
            loginUrl = "https://chatglm.cn/login",
            chatUrl = "https://chatglm.cn",
            apiBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
            tokenType = TokenType.SESSION,
            tokenExtractMethod = TokenExtractMethod.COOKIE_INTERCEPT,
            models = listOf(
                ModelInfo("glm-4", "GLM-4", 4096, true),
                ModelInfo("glm-4v", "GLM-4V", 4096),
                ModelInfo("glm-3-turbo", "GLM-3 Turbo", 4096)
            ),
            country = "china"
        )
    )
    
    fun getAllPlatforms(): List<AiPlatform> = platforms
    fun getGlobalPlatforms(): List<AiPlatform> = platforms.filter { it.country == "global" }
    fun getChinaPlatforms(): List<AiPlatform> = platforms.filter { it.country == "china" }
    fun getPlatform(id: String): AiPlatform? = platforms.find { it.id == id }
    fun getPlatformNames(): List<String> = platforms.map { it.name }
    fun getModelsForPlatform(platformId: String): List<ModelInfo> {
        return getPlatform(platformId)?.models ?: emptyList()
    }
}
