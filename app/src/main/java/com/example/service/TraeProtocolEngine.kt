package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class TraeProtocolEngine {

    suspend fun register(email: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@")) {
            RegistrationResult(success = true)
        } else {
            RegistrationResult(success = false, error = "Invalid email")
        }
    }

    suspend fun completeWithOtp(email: String, code: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                token = "trae_${System.currentTimeMillis()}_${Random.nextInt(1000,9999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid OTP")
        }
    }
}