package com.example.platform

data class AiPlatform(
    val id: String,
    val name: String,
    val loginUrl: String,
    val apiBaseUrl: String,
    val models: List<ModelDef>,
    val tokenType: TokenType = TokenType.COOKIE,
    val jsExtractCode: String? = null
)

data class ModelDef(
    val id: String,
    val name: String,
    val maxTokens: Int = 4096
)

enum class TokenType { COOKIE, BEARER, API_KEY }

object PlatformRegistry {
    val platforms = listOf(
        AiPlatform(
            id = "deepseek", name = "DeepSeek",
            loginUrl = "https://chat.deepseek.com/auth/login",
            apiBaseUrl = "https://api.deepseek.com/v1",
            models = listOf(ModelDef("deepseek-chat", "DeepSeek V3", 65536))
        ),
        AiPlatform(
            id = "openai", name = "ChatGPT",
            loginUrl = "https://chatgpt.com/auth/login",
            apiBaseUrl = "https://api.openai.com/v1",
            models = listOf(
                ModelDef("gpt-4o", "GPT-4o", 128000),
                ModelDef("gpt-4o-mini", "GPT-4o Mini", 128000)
            )
        ),
        AiPlatform(
            id = "claude", name = "Claude",
            loginUrl = "https://claude.ai/login",
            apiBaseUrl = "https://api.anthropic.com/v1",
            tokenType = TokenType.COOKIE,
            models = listOf(
                ModelDef("claude-sonnet-4-20250514", "Claude Sonnet 4", 200000),
                ModelDef("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", 200000)
            )
        ),
        AiPlatform(
            id = "gemini", name = "Gemini",
            loginUrl = "https://gemini.google.com",
            apiBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            tokenType = TokenType.COOKIE,
            models = listOf(ModelDef("gemini-2.0-flash", "Gemini 2.0 Flash", 1000000))
        ),
        AiPlatform(
            id = "groq", name = "Groq",
            loginUrl = "https://console.groq.com/login",
            apiBaseUrl = "https://api.groq.com/openai/v1",
            tokenType = TokenType.API_KEY,
            models = listOf(
                ModelDef("llama-3.3-70b-versatile", "Llama 3.3 70B", 32768),
                ModelDef("mixtral-8x7b-32768", "Mixtral 8x7B", 32768)
            )
        ),
        AiPlatform(
            id = "kimi", name = "Kimi",
            loginUrl = "https://kimi.moonshot.cn/login",
            apiBaseUrl = "https://api.moonshot.cn/v1",
            models = listOf(ModelDef("moonshot-v1-8k", "Kimi 8K", 8192))
        )
    )

    fun getById(id: String): AiPlatform? = platforms.find { it.id == id }
    fun findPlatformByModel(modelId: String): AiPlatform? =
        platforms.find { p -> p.models.any { it.id == modelId } }
    fun getAllModels() = platforms.flatMap { p -> p.models.map { it.id } }
}