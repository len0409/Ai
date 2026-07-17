package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*

class TodoListTool : Tool {
    private val tasks = mutableListOf<TodoItem>()
    private var nextId = 1

    override val name = "todo_write"
    override val description = "Create and manage a task list for your current coding session. Use action='add'/'update'/'list'/'clear'."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "action" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("One of: add, update, list, clear")
            )),
            "id" to JsonObject(mapOf(
                "type" to JsonPrimitive("integer"),
                "description" to JsonPrimitive("Task ID (required for 'update')")
            )),
            "content" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Task description (required for 'add', optional for 'update')")
            )),
            "status" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Task status: pending, in_progress, completed, cancelled (for 'update')")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("action")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val action = arguments["action"]?.jsonPrimitive?.content
            ?: return ToolResult("", "missing required parameter: action", true)

        return try {
            when (action) {
                "add" -> {
                    val content = arguments["content"]?.jsonPrimitive?.content
                        ?: return ToolResult("", "missing 'content' for add", true)
                    val task = TodoItem(nextId++, content, "pending")
                    tasks.add(task)
                    ToolResult("", "added task #${task.id}: ${task.content}")
                }
                "update" -> {
                    val id = arguments["id"]?.jsonPrimitive?.intOrNull
                        ?: return ToolResult("", "missing 'id' for update", true)
                    val idx = tasks.indexOfFirst { it.id == id }
                    if (idx == -1) return ToolResult("", "task #$id not found", true)
                    val status = arguments["status"]?.jsonPrimitive?.content
                    val content = arguments["content"]?.jsonPrimitive?.content
                    val task = tasks[idx]
                    val updated = task.copy(
                        content = content ?: task.content,
                        status = status ?: task.status
                    )
                    tasks[idx] = updated
                    ToolResult("", "updated task #${id}: ${updated.content} [${updated.status}]")
                }
                "list" -> {
                    if (tasks.isEmpty()) return ToolResult("", "(no tasks)")
                    val sb = StringBuilder()
                    val byStatus = tasks.groupBy { it.status }
                    for (status in listOf("in_progress", "pending", "completed", "cancelled")) {
                        byStatus[status]?.forEach { sb.appendLine("  #${it.id} [${status}] ${it.content}") }
                    }
                    ToolResult("", sb.toString().trimEnd())
                }
                "clear" -> {
                    val count = tasks.size
                    tasks.clear()
                    nextId = 1
                    ToolResult("", "cleared $count tasks")
                }
                else -> ToolResult("", "unknown action: $action", true)
            }
        } catch (e: Exception) {
            ToolResult("", "todo error: ${e.message}", true)
        }
    }

    private data class TodoItem(val id: Int, val content: String, val status: String)
}
