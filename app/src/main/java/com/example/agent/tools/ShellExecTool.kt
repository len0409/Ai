package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit

class ShellExecTool : Tool {
    override val name = "shell_exec"
    override val description = "Execute a shell command. Returns stdout and stderr output. Timeout is 30 seconds."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "command" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("The shell command to execute")
            )),
            "workdir" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Optional working directory for the command")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("command")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val command = arguments["command"]?.jsonPrimitive?.content
            ?: return@withContext ToolResult("", "missing required parameter: command", true)
        val workdir = arguments["workdir"]?.jsonPrimitive?.content

        return@withContext try {
            val pb = ProcessBuilder("sh", "-c", command)
            if (workdir != null) pb.directory(java.io.File(workdir))
            pb.redirectErrorStream(true)

            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText()
            val completed = process.waitFor(30, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext ToolResult("", "$output\n\n[command timed out after 30s]", true)
            }
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                ToolResult("", "$output\n\n[exit code: $exitCode]", true)
            } else {
                ToolResult("", output.ifBlank { "(no output)" })
            }
        } catch (e: Exception) {
            ToolResult("", "exec error: ${e.message}", true)
        }
    }
}
