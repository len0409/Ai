package com.example.service

interface SmsService {
    suspend fun getBalance(): Double
    suspend fun waitForCode(orderId: String, timeoutMs: Long): String?
}