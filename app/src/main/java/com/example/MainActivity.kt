package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppNavGraph
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val app = application as AiRelayApp
            setContent { AppNavGraph(container = app.container) }
        } catch (e: Throwable) {
            try { File(filesDir, "crash_log.txt").writeText(e.stackTraceToString()) } catch (_: Exception) {}
            setContent { ErrorScreen(e) }
        }
    }
}

@Composable
private fun ErrorScreen(e: Throwable) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("应用启动失败", color = Color(0xFFFF1744), fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "${e.javaClass.simpleName}: ${e.message ?: ""}\n\n${e.stackTraceToString()}",
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
