package com.example

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.navigation.AppNavGraph
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val app = application
            if (app !is AiRelayApp) {
                showError("Application 类型错误: ${app.javaClass.name}")
                return
            }
            val container = app.container
            setContent {
                AppNavGraph(container = container)
            }
        } catch (e: Throwable) {
            try {
                File(filesDir, "crash_log.txt").writeText(e.stackTraceToString())
            } catch (_: Exception) {}
            showError(e.stackTraceToString())
        }
    }

    private fun showError(msg: String) {
        val tv = TextView(this).apply {
            text = "启动失败:\n\n$msg"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(48, 96, 48, 48)
            textSize = 12f
        }
        setContentView(tv)
    }
}
