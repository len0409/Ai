package com.example.service

object SmsServiceFactory {
    fun create(provider: String, apiKey: String): SmsService {
        return when (provider.lowercase()) {
            "sms-activate" -> SmsActivateServiceImpl(apiKey)
            else -> SmsActivateServiceImpl(apiKey)
        }
    }
}