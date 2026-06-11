package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class GroqEngine {

    suspend fun register(email: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@")) {
            RegistrationResult(success = true)
        } else {
            RegistrationResult(success = false, error = "Invalid email")
        }
    }

    suspend fun completeWithCode(email: String, code: String, password: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                apiKey = "gsk_${System.currentTimeMillis()}${Random.nextInt(10000,99999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid verification code")
        }
    }
}