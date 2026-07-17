package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File

class FileWriteTool : Tool {
    override val name = "file_write"
    override val description = "Write content to a file. Creates parent directories if needed."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "path" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Absolute path to the file to write")
            )),
            "content" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Content to write to the file")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("path"), JsonPrimitive("content")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val path = arguments["path"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: path", true)
        val content = arguments["content"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: content", true)

        return try {
            val file = File(path).canonicalFile
            file.parentFile?.mkdirs()
            file.writeText(content)
            ToolResult("", "successfully wrote ${content.length} bytes to $path")
        } catch (e: Exception) {
            ToolResult("", "write error: ${e.message}", true)
        }
    }
}
