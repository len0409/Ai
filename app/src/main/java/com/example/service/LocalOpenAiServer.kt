package com.example.service

import android.content.Context
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.coroutines.*

class LocalOpenAiServer(private val context: Context, private val port: Int) {

    private var server: ApplicationEngine? = null

    fun start() {
        if (server != null) return
        server = embeddedServer(Netty, port = port) {
            routing {
                get("/health") {
                    call.respondText("OK", ContentType.Text.Plain)
                }
                get("/v1/models") {
                    call.respondText("[]", ContentType.Application.Json)
                }
                post("/v1/chat/completions") {
                    call.respondText(
                        """{"error":"not_implemented","message":"Gateway running, forwarder not yet connected"}""",
                        ContentType.Application.Json
                    )
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}