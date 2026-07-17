package com.example

import android.app.Application
import com.example.di.AppContainer
import java.io.File

class AiRelayApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                File(filesDir, "crash_log.txt").writeText(
                    "${System.currentTimeMillis()}\n${e.stackTraceToString()}"
                )
            } catch (_: Exception) {}
        }
        container = AppContainer(this)
    }
}
