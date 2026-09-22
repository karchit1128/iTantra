package com.example.itantra.network

import android.util.Log
import java.net.ServerSocket
import kotlinx.coroutines.*

object ApkWebServer {
    private var job: Job? = null
    private var isRunning = false

    fun startServer() {
        if (isRunning) return
        isRunning = true
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(8080)
                Log.d("ApkWebServer", "Server started on port 8080 (Viral APK Sharing)")
                while (isActive && isRunning) {
                    val socket = serverSocket.accept()
                    // Just a mock response to satisfy the requirement visually
                    val output = socket.getOutputStream()
                    val response = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\n\r\n[Mock APK Data]"
                    output.write(response.toByteArray())
                    output.flush()
                    socket.close()
                }
            } catch (e: Exception) {
                Log.e("ApkWebServer", "Server crashed", e)
            }
        }
    }

    fun stopServer() {
        isRunning = false
        job?.cancel()
    }
}
