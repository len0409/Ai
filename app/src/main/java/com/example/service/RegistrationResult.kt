package com.example.service

data class RegistrationResult(
    val success: Boolean,
    val error: String? = null,
    val password: String? = null,
    val accessToken: String? = null,
    val apiKey: String? = null,
    val token: String? = null,
    val sessionId: String? = null
)