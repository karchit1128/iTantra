with open("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("    fun broadcastMessage(message: String) {", """    fun sendWithRetry(msgId: String, message: String) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            var retries = 0
            while (retries < 4) { // Initial + 3 retries
                broadcastMessage(message)
                delay(2000)
                if (triageRepo.isAcked(msgId)) {
                    val latency = System.currentTimeMillis() - startTime
                    Log.i(TAG, "STT_ACK_LATENCY: Message $msgId ACKED after $retries retries. Latency: ${latency}ms")
                    return@launch
                }
                retries++
                Log.w(TAG, "STT_ACK_RETRY: Message $msgId missing ACK. Retry $retries...")
            }
            Log.e(TAG, "STT_ACK_FAILED: Message $msgId failed after 3 retries (No ACK).")
        }
    }

    fun broadcastMessage(message: String) {""")

with open("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", "w", encoding="utf-8") as f:
    f.write(code)
