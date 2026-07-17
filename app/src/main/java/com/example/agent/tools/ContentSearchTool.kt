package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File

class ContentSearchTool : Tool {
    override val name = "content_search"
    override val description = "Search for a regex pattern in files. Returns matching file paths and line numbers."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "pattern" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Regex pattern to search for in file contents")
            )),
            "directory" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Base directory to search in")
            )),
            "filePattern" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Optional file extension filter (e.g. '.kt', '.py')")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("pattern")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val pattern = arguments["pattern"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: pattern", true)
        val directory = arguments["directory"]?.jsonPrimitive?.content ?: "/sdcard"
        val filePattern = arguments["filePattern"]?.jsonPrimitive?.content

        return try {
            val base = File(directory)
            if (!base.exists()) return ToolResult("", "directory not found: $directory", true)

            val regex = try { Regex(pattern, setOf(RegexOption.IGNORE_CASE)) } catch (e: Exception) {
                return ToolResult("", "invalid regex: ${e.message}", true)
            }

            val results = mutableListOf<String>()
            searchInDir(base, regex, filePattern, results, depth = 5, maxResults = 100)

            if (results.isEmpty()) {
                ToolResult("", "no matches found for: $pattern")
            } else {
                ToolResult("", "Found ${results.size} matches:\n${results.joinToString("\n")}")
            }
        } catch (e: Exception) {
            ToolResult("", "search error: ${e.message}", true)
        }
    }

    private fun searchInDir(dir: File, regex: Regex, filePattern: String?, results: MutableList<String>, depth: Int, maxResults: Int) {
        if (depth <= 0 || results.size >= maxResults) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= maxResults) return
            if (file.isDirectory && !file.name.startsWith(".")) {
                searchInDir(file, regex, filePattern, results, depth - 1, maxResults)
            } else if (file.isFile) {
                if (filePattern != null && !file.name.endsWith(filePattern)) continue
                try {
                    file.forEachLine { line ->
                        if (regex.containsMatchIn(line)) {
                            results.add("${file.absolutePath}: ${line.take(200)}")
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
