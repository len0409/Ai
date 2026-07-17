package com.example

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.i("AiRelay", "Starting...")
            showText("AI Relay", "启动中...\n\n如果此页面不消失，说明应用初始化成功。")

            val app = application as AiRelayApp
            Log.i("AiRelay", "AppContainer loaded")
            showText("AI Relay", "Container 就绪")

            val tokenRepo = app.container.tokenRepository
            Log.i("AiRelay", "tokenRepo=$tokenRepo")
            showText("AI Relay", "数据库就绪")
        } catch (e: Throwable) {
            Log.e("AiRelay", "Startup failed", e)
            showError(e)
        }
    }

    private fun showError(e: Throwable) {
        val msg = "${e.javaClass.name}: ${e.message}\n\n${e.stackTraceToString()}"
        showText("启动失败", msg)
    }

    private fun showText(title: String, body: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(0xFF0D1117.toInt())
        }
        root.addView(TextView(this).apply {
            text = title
            setTextColor(0xFFE6EDF3.toInt())
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = body
            setTextColor(0xFF8B949E.toInt())
            textSize = 12f
            setPadding(0, 24, 0, 0)
        })
        val sv = ScrollView(this).apply { addView(root) }
        setContentView(sv)
    }
}
