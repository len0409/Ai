package com.example.service

val MODEL_CATALOG: List<ModelEntry> = listOf(
    ModelEntry("gpt-3.5-turbo", "GPT-3.5 Turbo", "OpenAI", "standard"),
    ModelEntry("gpt-4", "GPT-4", "OpenAI", "premium"),
    ModelEntry("gpt-4-turbo", "GPT-4 Turbo", "OpenAI", "premium"),
    ModelEntry("gpt-4o", "GPT-4o", "OpenAI", "premium"),
    ModelEntry("deepseek-chat", "DeepSeek Chat", "DeepSeek", "standard"),
    ModelEntry("deepseek-coder", "DeepSeek Coder", "DeepSeek", "standard"),
    ModelEntry("llama3-8b-8192", "Llama 3 8B", "Groq", "standard"),
    ModelEntry("llama3-70b-8192", "Llama 3 70B", "Groq", "standard"),
    ModelEntry("mixtral-8x7b-32768", "Mixtral 8x7B", "Groq", "standard"),
    ModelEntry("gemini-pro", "Gemini Pro", "Gemini", "standard"),
    ModelEntry("gemini-pro-vision", "Gemini Pro Vision", "Gemini", "standard"),
    ModelEntry("claude-3-haiku-20240307", "Claude 3 Haiku", "Claude", "standard"),
    ModelEntry("claude-3-sonnet-20240229", "Claude 3 Sonnet", "Claude", "premium"),
    ModelEntry("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Claude", "premium"),
    ModelEntry("llama3.1-8b", "Llama 3.1 8B", "Cerebras", "standard"),
    ModelEntry("llama3.1-70b", "Llama 3.1 70B", "Cerebras", "premium"),
    ModelEntry("kiro-default", "Kiro Default", "Kiro", "standard"),
)