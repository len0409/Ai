package com.example.agent.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class ClipboardTool(private val context: Context) : Tool {
    override val name = "clipboard"
    override val description = "Read from or write to the device clipboard. Use action='read' or action='write'."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "action" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("'read' to get clipboard content, 'write' to set clipboard")
            )),
            "text" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Text to write to clipboard (required for action='write')")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("action")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.Main) {
        val action = arguments["action"]?.jsonPrimitive?.content
            ?: return@withContext ToolResult("", "missing required parameter: action", true)

        return@withContext try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            when (action) {
                "read" -> {
                    val clip = cm.primaryClip
                    if (clip == null || clip.itemCount == 0) {
                        ToolResult("", "(clipboard is empty)")
                    } else {
                        val text = clip.getItemAt(0).text?.toString() ?: "(non-text content)"
                        ToolResult("", text.take(10000))
                    }
                }
                "write" -> {
                    val text = arguments["text"]?.jsonPrimitive?.content
                        ?: return@withContext ToolResult("", "missing 'text' parameter for write", true)
                    cm.setPrimaryClip(ClipData.newPlainText("agent", text))
                    ToolResult("", "clipboard set (${text.length} chars)")
                }
                else -> ToolResult("", "unknown action: $action (use 'read' or 'write')", true)
            }
        } catch (e: Exception) {
            ToolResult("", "clipboard error: ${e.message}", true)
        }
    }
}
