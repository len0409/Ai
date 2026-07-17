package com.example.agent.tools

import com.example.agent.Tool
import com.example.agent.ToolResult
import kotlinx.serialization.json.*
import java.io.File
import java.net.NetworkInterface

class DeviceInfoTool : Tool {
    override val name = "device_info"
    override val description = "Get device information including OS version, available storage, network status, and running processes."
    override val parameters = JsonObject(mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(mapOf(
            "info_type" to JsonObject(mapOf(
                "type" to JsonPrimitive("string"),
                "description" to JsonPrimitive("Type of info: system, storage, network, all (default: all)")
            ))
        )),
        "required" to JsonArray(emptyList())
    ))

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val type = arguments["info_type"]?.jsonPrimitive?.content ?: "all"
        return try {
            val sb = StringBuilder()

            if (type == "all" || type == "system") {
                sb.appendLine("=== System ===")
                sb.appendLine("OS: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                sb.appendLine("CPU ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString(", ")}")
                sb.appendLine("Available processors: ${Runtime.getRuntime().availableProcessors()}")
                val maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024
                val totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024
                val freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024
                sb.appendLine("Memory: ${freeMem}MB free / ${totalMem}MB total / ${maxMem}MB max (JVM)")
                sb.appendLine()
            }

            if (type == "all" || type == "storage") {
                sb.appendLine("=== Storage ===")
                val root = File("/")
                sb.appendLine("Root total: ${root.totalSpace / 1024 / 1024 / 1024}GB")
                sb.appendLine("Root free: ${root.freeSpace / 1024 / 1024 / 1024}GB")
                val data = File("/data")
                if (data.exists()) {
                    sb.appendLine("/data total: ${data.totalSpace / 1024 / 1024 / 1024}GB")
                    sb.appendLine("/data free: ${data.freeSpace / 1024 / 1024 / 1024}GB")
                }
                sb.appendLine()
            }

            if (type == "all" || type == "network") {
                sb.appendLine("=== Network ===")
                try {
                    NetworkInterface.getNetworkInterfaces().toList().forEach { iface ->
                        if (!iface.isLoopback && iface.isUp) {
                            val addrs = iface.inetAddresses.toList().filter { !it.isLoopbackAddress }
                            if (addrs.isNotEmpty()) {
                                sb.appendLine("${iface.name}: ${addrs.joinToString { it.hostAddress ?: "?" }}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    sb.appendLine("network: unavailable")
                }
            }

            ToolResult("", sb.toString())
        } catch (e: Exception) {
            ToolResult("", "device info error: ${e.message}", true)
        }
    }
}
