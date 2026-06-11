package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class ClaudeEngine(private val smsService: SmsService) {

    var phoneOrderId: String = ""

    suspend fun register(email: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@")) {
            RegistrationResult(success = true)
        } else {
            RegistrationResult(success = false, error = "Invalid email")
        }
    }

    suspend fun continueWithEmailCode(email: String, code: String): Boolean {
        delay(1000)
        return code.length >= 4
    }

    suspend fun buyAndSendPhoneCode(): Boolean {
        delay(2000)
        phoneOrderId = "claude_order_${System.currentTimeMillis()}"
        return true
    }

    suspend fun completeWithPhoneCode(password: String, code: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                apiKey = "sk-ant-${System.currentTimeMillis()}${Random.nextInt(1000,9999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid phone code")
        }
    }
}