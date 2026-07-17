package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File

class FileSearchTool : Tool {
    override val name = "file_search"
    override val description = "Search for files matching a glob pattern. Returns relative file paths."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "pattern" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Glob pattern like **/*.kt or src/**/*.java")
            )),
            "directory" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Base directory to search in (default: /sdcard)")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("pattern")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val pattern = arguments["pattern"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: pattern", true)
        val directory = arguments["directory"]?.jsonPrimitive?.content ?: "/sdcard"

        return try {
            val base = File(directory)
            if (!base.exists()) return ToolResult("", "directory not found: $directory", true)

            val results = mutableListOf<String>()
            val regex = globToRegex(pattern)
            walkFiles(base, base.absolutePath, regex, results, depth = 5, maxResults = 200)

            if (results.isEmpty()) {
                ToolResult("", "no files matched pattern: $pattern")
            } else {
                ToolResult("", "Found ${results.size} files:\n${results.joinToString("\n")}")
            }
        } catch (e: Exception) {
            ToolResult("", "search error: ${e.message}", true)
        }
    }

    private fun walkFiles(dir: File, basePath: String, regex: Regex, results: MutableList<String>, depth: Int, maxResults: Int) {
        if (depth <= 0 || results.size >= maxResults) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= maxResults) return
            if (file.isDirectory && !file.name.startsWith(".")) {
                walkFiles(file, basePath, regex, results, depth - 1, maxResults)
            } else if (regex.matches(file.name) || regex.matches(file.absolutePath.removePrefix(basePath).removePrefix("/"))) {
                results.add(file.absolutePath)
            }
        }
    }

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                '.' -> sb.append("\\.")
                '\\' -> sb.append("\\\\")
                else -> sb.append(c)
            }
        }
        sb.append("$")
        return Regex(sb.toString())
    }
}
