package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppNavGraph
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { enableEdgeToEdge() } catch (_: Exception) {}
        try {
            val container = (application as AiRelayApp).container
            setContent { AppNavGraph(container = container) }
        } catch (e: Throwable) {
            try {
                File(filesDir, "crash_log.txt").writeText(
                    "${System.currentTimeMillis()}\n${e.stackTraceToString()}"
                )
            } catch (_: Exception) {}
            setContent { CrashScreen(error = e, activity = this) }
        }
    }
}

@Composable
private fun CrashScreen(error: Throwable, activity: ComponentActivity) {
    val context = LocalContext.current
    MaterialTheme(colorScheme = darkColorScheme(), content = {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("应用启动失败", color = Color(0xFFFF1744), fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Text("${error.javaClass.simpleName}: ${error.message ?: ""}",
                color = Color(0xFFE6EDF3), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(error.stackTraceToString(), color = Color(0xFF8B949E),
                fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                activity.finishAffinity()
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage(context.packageName)!!.apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }) {
                Text("重启应用")
            }
        }
    })
}
