package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class DeepSeekEngine {

    suspend fun register(email: String, password: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@") && password.length >= 8) {
            RegistrationResult(success = true)
        } else {
            RegistrationResult(success = false, error = "Invalid email or password")
        }
    }

    suspend fun completeWithCode(email: String, code: String, password: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                apiKey = "sk-deepseek-${System.currentTimeMillis()}${Random.nextInt(1000,9999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid verification code")
        }
    }
}