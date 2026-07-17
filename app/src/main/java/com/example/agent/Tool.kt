package com.example.agent

import kotlinx.serialization.json.*

data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false
)

interface Tool {
    val name: String
    val description: String
    val parameters: JsonObject

    suspend fun execute(arguments: JsonObject): ToolResult

    fun toToolJson(): JsonObject = JsonObject(mapOf(
        "type" to JsonPrimitive("function"),
        "function" to JsonObject(mapOf(
            "name" to JsonPrimitive(name),
            "description" to JsonPrimitive(description),
            "parameters" to parameters
        ))
    ))
}
