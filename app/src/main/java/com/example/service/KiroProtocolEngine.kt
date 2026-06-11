package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class KiroProtocolEngine {

    suspend fun register(email: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@")) {
            RegistrationResult(
                success = true,
                password = "Kiro_" + (100000..999999).random().toString()
            )
        } else {
            RegistrationResult(success = false, error = "Invalid email")
        }
    }

    suspend fun completeWithOtp(code: String, email: String, password: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                accessToken = "kiro_at_${System.currentTimeMillis()}_${Random.nextInt(1000,9999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid OTP")
        }
    }
}