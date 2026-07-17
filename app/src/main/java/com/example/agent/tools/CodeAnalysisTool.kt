package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

class CodeAnalysisTool : Tool {
    override val name = "code_analyze"
    override val description = "Analyze code structure: find imports, classes, functions, and dependencies. Supports Kotlin, Java, Python, JS/TS, Go, Rust."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "path" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("File or directory path to analyze")
            )),
            "analysis" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Analysis type: imports, functions, classes, structure, deps (default: structure)")
            )),
            "language" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Language hint: kotlin, java, python, javascript, typescript, go, rust (auto-detected if omitted)")
            ))
        )),
        "required" to JsonArray(listOf(JsonPrimitive("path")))
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = arguments["path"]?.jsonPrimitive?.content
            ?: return@withContext ToolResult("", "missing required parameter: path", true)
        val analysis = arguments["analysis"]?.jsonPrimitive?.content ?: "structure"
        val language = arguments["language"]?.jsonPrimitive?.content

        return@withContext try {
            val file = File(path)
            if (!file.exists()) return@withContext ToolResult("", "path not found: $path", true)

            if (file.isFile) {
                analyzeFile(file, analysis, language)
            } else {
                analyzeDirectory(file, analysis, language)
            }
        } catch (e: Exception) {
            ToolResult("", "analysis error: ${e.message}", true)
        }
    }

    private fun analyzeFile(file: File, analysis: String, lang: String?): ToolResult {
        val text = file.readText()
        val detected = lang ?: detectLanguage(file.name)
        val sb = StringBuilder()

        when (analysis) {
            "imports" -> sb.appendLine(extractImports(text, detected))
            "functions" -> sb.appendLine(extractFunctions(text, detected))
            "classes" -> sb.appendLine(extractClasses(text, detected))
            "structure" -> {
                sb.appendLine("=== ${file.name} (${file.length() / 1024}KB, ${text.lines().size} lines, $detected) ===")
                sb.appendLine()
                val imports = extractImports(text, detected)
                if (imports.isNotBlank()) { sb.appendLine("Imports:"); sb.appendLine(imports); sb.appendLine() }
                val classes = extractClasses(text, detected)
                if (classes.isNotBlank()) { sb.appendLine("Classes/Interfaces:"); sb.appendLine(classes); sb.appendLine() }
                val funcs = extractFunctions(text, detected)
                if (funcs.isNotBlank()) { sb.appendLine("Functions/Methods:"); sb.appendLine(funcs) }
            }
            "deps" -> sb.appendLine(extractImports(text, detected))
            else -> return ToolResult("", "unknown analysis type: $analysis", true)
        }

        return ToolResult("", sb.toString().trimEnd().ifBlank { "(nothing found)" })
    }

    private fun analyzeDirectory(dir: File, analysis: String, lang: String?): ToolResult {
        val files = mutableListOf<File>()
        collectSourceFiles(dir, files, depth = 3, maxFiles = 100)
        if (files.isEmpty()) return ToolResult("", "no source files found in $dir")

        val sb = StringBuilder()
        sb.appendLine("=== Directory: ${dir.name} (${files.size} source files) ===")
        sb.appendLine()

        val byExt = files.groupBy { it.extension.lowercase() }
        sb.appendLine("File types:")
        for ((ext, list) in byExt) { sb.appendLine("  .$ext: ${list.size} files") }
        sb.appendLine()

        sb.appendLine("File tree (depth 3):")
        buildFileTree(dir, dir, 0, 3, sb)
        sb.appendLine()

        if (analysis == "deps" || analysis == "structure") {
            sb.appendLine("Top-level definitions:")
            for (file in files.take(20)) {
                val langDetected = detectLanguage(file.name)
                val defs = extractTopLevelDefinitions(file.readText(), langDetected)
                if (defs.isNotBlank()) {
                    sb.appendLine("  ${file.name}: $defs")
                }
            }
        }

        return ToolResult("", sb.toString().trimEnd())
    }

    private fun buildFileTree(base: File, dir: File, depth: Int, maxDepth: Int, sb: StringBuilder) {
        if (depth > maxDepth) return
        val files = dir.listFiles()?.sortedBy { it.name } ?: return
        for (file in files) {
            val indent = "  ".repeat(depth)
            if (file.isDirectory && !file.name.startsWith(".")) {
                sb.appendLine("$indent${file.name}/")
                buildFileTree(base, file, depth + 1, maxDepth, sb)
            } else if (file.isFile && !file.name.startsWith(".")) {
                sb.appendLine("$indent${file.name}")
            }
        }
    }

    private fun collectSourceFiles(dir: File, result: MutableList<File>, depth: Int, maxFiles: Int) {
        if (depth <= 0 || result.size >= maxFiles) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (result.size >= maxFiles) return
            if (file.isDirectory && !file.name.startsWith(".")) {
                collectSourceFiles(file, result, depth - 1, maxFiles)
            } else if (file.isFile && isSourceFile(file.name)) {
                result.add(file)
            }
        }
    }

    private fun isSourceFile(name: String): Boolean {
        val exts = setOf("kt", "kts", "java", "py", "js", "jsx", "ts", "tsx", "go", "rs", "c", "cpp", "h", "hpp", "swift", "rb", "php", "sh", "bash", "yaml", "yml", "json", "xml", "gradle", "properties", "toml")
        return name.contains(".") && exts.contains(name.substringAfterLast(".").lowercase())
    }

    private fun detectLanguage(filename: String): String = when {
        filename.endsWith(".kt") || filename.endsWith(".kts") -> "kotlin"
        filename.endsWith(".java") -> "java"
        filename.endsWith(".py") -> "python"
        filename.endsWith(".js") || filename.endsWith(".jsx") -> "javascript"
        filename.endsWith(".ts") || filename.endsWith(".tsx") -> "typescript"
        filename.endsWith(".go") -> "go"
        filename.endsWith(".rs") -> "rust"
        filename.endsWith(".c") || filename.endsWith(".cpp") || filename.endsWith(".h") -> "c_cpp"
        else -> "unknown"
    }

    private fun extractImports(text: String, lang: String): String {
        val pattern = when (lang) {
            "kotlin", "java" -> Regex("^import\\s+([\\w.]+)", RegexOption.MULTILINE)
            "python" -> Regex("^(?:from\\s+(\\S+)\\s+)?import\\s+(.+)", RegexOption.MULTILINE)
            "javascript", "typescript" -> Regex("^(?:import\\s+.*?from\\s+['\"]([^'\"]+)['\"]|require\\(['\"]([^'\"]+)['\"]\\))", RegexOption.MULTILINE)
            "go" -> Regex("^\\s*\"([^\"]+)\"", RegexOption.MULTILINE)
            "rust" -> Regex("^use\\s+([\\w:]+)", RegexOption.MULTILINE)
            else -> return "  (unsupported language)"
        }
        val matches = pattern.findAll(text).take(50).map { it.value.trim() }.joinToString("\n")
        return if (matches.isNotEmpty()) matches else "  (none found)"
    }

    private fun extractFunctions(text: String, lang: String): String {
        val pattern = when (lang) {
            "kotlin" -> Regex("""^\s*(?:suspend\s+)?(?:override\s+)?fun\s+(\w+)""", RegexOption.MULTILINE)
            "java" -> Regex("""^\s*(?:public|private|protected|static|\s)+[\w<>[\],\s]+\s+(\w+)\s*\([^)]*\)\s*\{""", RegexOption.MULTILINE)
            "python" -> Regex("""^\s*def\s+(\w+)\s*\(([^)]*)\)""", RegexOption.MULTILINE)
            "javascript", "typescript" -> Regex("""(?:function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?\([^)]*\)\s*=>)""", RegexOption.MULTILINE)
            "go" -> Regex("""^func\s+(?:\(\w+\s+\*?\w+\)\s+)?(\w+)""", RegexOption.MULTILINE)
            "rust" -> Regex("""^fn\s+(\w+)""", RegexOption.MULTILINE)
            else -> return "  (unsupported language)"
        }
        val matches = pattern.findAll(text).take(50).map { it.value.trim().take(100) }.joinToString("\n")
        return if (matches.isNotEmpty()) matches else "  (none found)"
    }

    private fun extractClasses(text: String, lang: String): String {
        val pattern = when (lang) {
            "kotlin" -> Regex("""^\s*(?:data\s+)?(?:sealed\s+)?(?:abstract\s+)?(?:class|interface|object|enum\s+class)\s+(\w+)""", RegexOption.MULTILINE)
            "java" -> Regex("""^\s*(?:public\s+)?(?:abstract\s+)?(?:class|interface|enum)\s+(\w+)""", RegexOption.MULTILINE)
            "python" -> Regex("""^\s*class\s+(\w+)""", RegexOption.MULTILINE)
            "javascript", "typescript" -> Regex("""^\s*(?:export\s+)?(?:class|interface|type)\s+(\w+)""", RegexOption.MULTILINE)
            "go" -> Regex("""^type\s+(\w+)\s+struct""", RegexOption.MULTILINE)
            "rust" -> Regex("""^(?:pub\s+)?(?:struct|enum|trait|impl)\s+(\w+)""", RegexOption.MULTILINE)
            else -> return "  (unsupported language)"
        }
        val matches = pattern.findAll(text).take(50).map { it.value.trim().take(100) }.joinToString("\n")
        return if (matches.isNotEmpty()) matches else "  (none found)"
    }

    private fun extractTopLevelDefinitions(text: String, lang: String): String {
        val classes = extractClasses(text, lang).lines().filter { it.isNotBlank() && !it.startsWith("  (") }
        val funcs = extractFunctions(text, lang).lines().filter { it.isNotBlank() && !it.startsWith("  (") }
        val all = (classes + funcs).take(10)
        return all.joinToString(", ")
    }
}
