package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File

class KnowledgeMemoryTool(private val storageDir: File = File("/data/local/tmp/ai_memory")) : Tool {
    override val name = "knowledge"
    override val description = "Persistent knowledge store: save, recall, search, and list facts across sessions. Use to remember preferences, decisions, and project context."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "action" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("One of: save, recall, search, list, delete, clear")
            )),
            "key" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Key for save/recall/delete operations")
            )),
            "value" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Value to save (required for save)")
            )),
            "query" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Search query for 'search' action (partial match)")
            )),
            "category" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Optional category tag: preference, decision, fact, context, codebase")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("action")))
    ))

    init { storageDir.mkdirs() }

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val action = arguments["action"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: action", true)

        return try {
            when (action) {
                "save" -> handleSave(arguments)
                "recall" -> handleRecall(arguments)
                "search" -> handleSearch(arguments)
                "list" -> handleList(arguments)
                "delete" -> handleDelete(arguments)
                "clear" -> handleClear()
                else -> ToolResult("", "unknown action: $action", true)
            }
        } catch (e: Exception) {
            ToolResult("", "knowledge error: ${e.message}", true)
        }
    }

    private fun entryFile(key: String): File = File(storageDir, "${key.hashCode().toString(36)}.json")

    private data class Entry(
        val key: String,
        val value: String,
        val category: String = "general",
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun handleSave(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing 'key' for save", true)
        val value = args["value"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing 'value' for save", true)
        val category = args["category"]?.jsonPrimitive?.content ?: "general"

        val entry = JsonObject(mapOf(
            "key" to JsonPrimitive(key),
            "value" to JsonPrimitive(value),
            "category" to JsonPrimitive(category),
            "timestamp" to JsonPrimitive(System.currentTimeMillis())
        ))
        entryFile(key).writeText(entry.toString())
        return ToolResult("", "saved: $key [$category]")
    }

    private fun handleRecall(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing 'key' for recall", true)
        val file = entryFile(key)
        if (!file.exists()) return ToolResult("", "no entry for key: $key", true)
        val json = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
            ?: return ToolResult("", "corrupted entry for key: $key", true)
        val value = json["value"]?.jsonPrimitive?.content ?: "(empty)"
        val category = json["category"]?.jsonPrimitive?.content ?: ""
        val ts = json["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L
        val age = (System.currentTimeMillis() - ts) / 1000 / 60
        return ToolResult("", "[$category] $value\n(aged ${age}m)")
    }

    private fun handleSearch(args: JsonObject): ToolResult {
        val query = args["query"]?.jsonPrimitive?.content?.lowercase()
            ?: return ToolResult("", "missing 'query' for search", true)
        val results = storageDir.listFiles()?.filter { it.name.endsWith(".json") }?.mapNotNull { file ->
            runCatching {
                val json = Json.parseToJsonElement(file.readText()).jsonObject
                val key = json["key"]?.jsonPrimitive?.content ?: ""
                val value = json["value"]?.jsonPrimitive?.content ?: ""
                if ((key + value).lowercase().contains(query)) {
                    "[${json["category"]?.jsonPrimitive?.content ?: ""}] $key: ${value.take(100)}"
                } else null
            }.getOrNull()
        } ?: emptyList()

        return if (results.isEmpty()) ToolResult("", "no matches for: $query")
        else ToolResult("", "Found ${results.size}:\n${results.joinToString("\n")}")
    }

    private fun handleList(args: JsonObject): ToolResult {
        val category = args["category"]?.jsonPrimitive?.content
        val entries = storageDir.listFiles()?.filter { it.name.endsWith(".json") }?.mapNotNull { file ->
            runCatching {
                val json = Json.parseToJsonElement(file.readText()).jsonObject
                val cat = json["category"]?.jsonPrimitive?.content ?: "general"
                if (category != null && cat != category) return@mapNotNull null
                val key = json["key"]?.jsonPrimitive?.content ?: ""
                val value = json["value"]?.jsonPrimitive?.content?.take(80) ?: ""
                "[$cat] $key: $value"
            }.getOrNull()
        }?.sorted() ?: emptyList()

        return if (entries.isEmpty()) ToolResult("", "(no knowledge entries)")
        else ToolResult("", "${entries.size} entries:\n${entries.joinToString("\n")}")
    }

    private fun handleDelete(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing 'key' for delete", true)
        val file = entryFile(key)
        return if (file.delete()) ToolResult("", "deleted: $key")
        else ToolResult("", "not found: $key", true)
    }

    private fun handleClear(): ToolResult {
        val count = storageDir.listFiles()?.filter { it.delete() }?.size ?: 0
        return ToolResult("", "cleared $count entries")
    }
}
