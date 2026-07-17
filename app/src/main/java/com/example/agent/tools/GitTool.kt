package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.util.concurrent.TimeUnit

class GitTool : Tool {
    override val name = "git"
    override val description = "Execute git operations: status, diff, log, branch, add, commit, blame, show. Works on repositories in the workspace directory."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "operation" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Git operation: status, diff, diff_staged, log, branch, branch_create, branch_switch, add, commit, blame, show, remote")
            )),
            "path" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("File path for add/blame/show operations")
            )),
            "message" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Commit message (required for commit)")
            )),
            "branch" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Branch name for branch_create/branch_switch")
            )),
            "count" to JsonObject(mapOf(
                "type" to JsonPrimitive("integer"),
                "description" to JsonPrimitive("Number of log entries (default: 10)")
            )),
            "directory" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Repository directory path (default: auto-detect)")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("operation")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val operation = arguments["operation"]?.jsonPrimitive?.content
            ?: return@withContext ToolResult("", "missing required parameter: operation", true)
        val dir = arguments["directory"]?.jsonPrimitive?.content ?: findGitDir()
            ?: return@withContext ToolResult("", "no git repository found in /sdcard or /data/local/tmp", true)

        return@withContext try {
            when (operation) {
                "status" -> execGit(dir, "status", "--short", "--branch")
                "diff" -> execGit(dir, "diff", "--unified=10", "--no-color")
                "diff_staged" -> execGit(dir, "diff", "--staged", "--unified=10", "--no-color")
                "log" -> {
                    val count = arguments["count"]?.jsonPrimitive?.intOrNull ?: 10
                    execGit(dir, "log", "--oneline", "--graph", "--decorate", "-$count", "--no-color")
                }
                "branch" -> execGit(dir, "branch", "--list", "-v")
                "branch_create" -> {
                    val branch = arguments["branch"]?.jsonPrimitive?.content
                        ?: return@withContext ToolResult("", "missing branch name", true)
                    execGit(dir, "checkout", "-b", branch)
                }
                "branch_switch" -> {
                    val branch = arguments["branch"]?.jsonPrimitive?.content
                        ?: return@withContext ToolResult("", "missing branch name", true)
                    execGit(dir, "checkout", branch)
                }
                "add" -> {
                    val path = arguments["path"]?.jsonPrimitive?.content ?: "."
                    execGit(dir, "add", path)
                }
                "commit" -> {
                    val msg = arguments["message"]?.jsonPrimitive?.content
                        ?: return@withContext ToolResult("", "missing commit message", true)
                    execGit(dir, "commit", "-m", msg)
                }
                "blame" -> {
                    val path = arguments["path"]?.jsonPrimitive?.content
                        ?: return@withContext ToolResult("", "missing file path", true)
                    execGit(dir, "blame", "--no-color", path)
                }
                "show" -> {
                    val path = arguments["path"]?.jsonPrimitive?.content ?: "HEAD"
                    execGit(dir, "show", "--stat", "--no-color", "--no-patch", path)
                }
                "remote" -> execGit(dir, "remote", "-v")
                else -> ToolResult("", "unknown operation: $operation. Use: status/diff/diff_staged/log/branch/branch_create/branch_switch/add/commit/blame/show/remote", true)
            }
        } catch (e: Exception) {
            ToolResult("", "git error: ${e.message}", true)
        }
    }

    private fun findGitDir(): String? {
        val candidates = listOf("/sdcard", "/sdcard/workspace", "/data/local/tmp/ai_workspace")
        for (dir in candidates) {
            val gitDir = File(dir, ".git")
            if (gitDir.exists() && gitDir.isDirectory) return dir
        }
        // Search subdirectories
        for (base in listOf("/sdcard", "/data/local/tmp")) {
            File(base).listFiles()?.forEach { sub ->
                val gitDir = File(sub, ".git")
                if (gitDir.exists() && gitDir.isDirectory) return sub.absolutePath
            }
        }
        return null
    }

    private fun execGit(dir: String, vararg args: String): ToolResult {
        val pb = ProcessBuilder("git", *args)
        pb.directory(File(dir))
        pb.redirectErrorStream(true)
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(15, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return ToolResult("", "$output\n[timeout]", true)
        }
        val isError = process.exitValue() != 0
        return ToolResult("", output.ifBlank { "(empty)" }, isError)
    }
}
