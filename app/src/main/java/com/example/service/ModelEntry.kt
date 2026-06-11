package com.example.service

data class ModelEntry(
    val id: String,
    val name: String,
    val provider: String,
    val tier: String = "standard"
)