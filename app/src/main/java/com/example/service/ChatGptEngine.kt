package com.example.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class ChatGptEngine(private val smsService: SmsService) {

    var phoneOrderId: String = ""

    suspend fun register(email: String, password: String): RegistrationResult {
        delay(2000)
        return if (email.contains("@") && password.length >= 8) {
            RegistrationResult(success = true)
        } else {
            RegistrationResult(success = false, error = "Invalid email or password")
        }
    }

    suspend fun continueWithEmailCode(email: String, code: String): Boolean {
        delay(1000)
        return code.length >= 4
    }

    suspend fun buyAndSendPhoneCode(): Boolean {
        delay(2000)
        phoneOrderId = "order_${System.currentTimeMillis()}"
        return true
    }

    suspend fun completeWithPhoneCode(password: String, code: String): RegistrationResult {
        delay(1500)
        return if (code.length >= 4) {
            RegistrationResult(
                success = true,
                accessToken = "chatgpt_at_${System.currentTimeMillis()}_${Random.nextInt(1000,9999)}"
            )
        } else {
            RegistrationResult(success = false, error = "Invalid phone code")
        }
    }
}