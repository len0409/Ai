package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File

class FileReadTool : Tool {
    override val name = "file_read"
    override val description = "Read the contents of a file. Returns the file content as text."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "path" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Absolute path to the file to read")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("path")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val path = arguments["path"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: path", true)

        val file = File(path).canonicalFile
        if (!file.exists()) return ToolResult("", "file not found: $path", true)
        if (!file.isFile) return ToolResult("", "not a file: $path", true)
        if (!file.canRead()) return ToolResult("", "cannot read file: $path", true)

        return try {
            val content = file.readText()
            if (content.length > 50000) {
                ToolResult("", content.take(50000) + "\n...(truncated at 50KB)")
            } else {
                ToolResult("", content)
            }
        } catch (e: Exception) {
            ToolResult("", "read error: ${e.message}", true)
        }
    }
}
