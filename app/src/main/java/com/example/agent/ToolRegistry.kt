package com.example.agent

import kotlinx.serialization.json.*

class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()
    private val toolDescriptions = mutableListOf<String>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
        val paramsDesc = tool.parameters["properties"]?.jsonObject?.keys?.joinToString(", ") ?: ""
        toolDescriptions.add("${tool.name}: ${tool.description} (params: $paramsDesc)")
    }

    fun get(name: String): Tool? = tools[name]
    fun getAll(): List<Tool> = tools.values.toList()
    fun getNames(): Set<String> = tools.keys.toSet()
    fun getToolListForPrompt(): String = toolDescriptions.joinToString("\n")

    fun toToolsJson(): kotlinx.serialization.json.JsonArray =
        kotlinx.serialization.json.JsonArray(tools.values.map { it.toToolJson() })

    fun clear() { tools.clear(); toolDescriptions.clear() }
}
