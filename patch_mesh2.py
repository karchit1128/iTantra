with open("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", "r", encoding="utf-8") as f:
    code = f.read()

# Replace msgPriority and parsing loop
search = """                var msgId = ""
                var msgPriority = "YELLOW"
                var actualMessageInner = plainText
                if (actualMessageInner.startsWith("[ID:")) {
                    val idEnd = actualMessageInner.indexOf("]")
                    if (idEnd != -1) {
                        msgId = actualMessageInner.substring(4, idEnd)
                        actualMessageInner = actualMessageInner.substring(idEnd + 1)
                        
                        if (triageRepo.victimExists(msgId)) {
                            return@launch
                        }
                        
                        broadcastMessage("[ACK:$msgId]")
                    }
                }
                
                if (actualMessageInner.startsWith("[PRIO:")) {
                    val pEnd = actualMessageInner.indexOf("]")
                    if (pEnd != -1) {
                        msgPriority = actualMessageInner.substring(6, pEnd)
                        actualMessageInner = actualMessageInner.substring(pEnd + 1)
                    }
                }
                
                if (actualMessageInner.startsWith("[ACK:")) {
                    val ackedId = actualMessageInner.substringAfter("[ACK:").substringBefore("]")
                    triageRepo.markAsAcked(ackedId)
                    return@launch
                }
                
                val rawMsg = actualMessageInner.removePrefix("[TTS]")
                val prefs = context.getSharedPreferences("itnt_settings", android.content.Context.MODE_PRIVATE)
                val targetLang = prefs.getString("target_language", "English") ?: "English"
                
                var translatedMsg = rawMsg
                try {
                    translatedMsg = com.example.itantra.ml.TranslationEngine.translate(rawMsg, "Unknown", targetLang)
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed", e)
                }
                
                val entity = TriageEntity(id = if(msgId.isNotEmpty()) msgId else java.util.UUID.randomUUID().toString(), message = translatedMsg, priority = msgPriority, isSentByMe = false)
                triageRepo.insertVictim(entity)
                _incomingMessages.tryEmit(if(actualMessageInner.startsWith("[TTS]")) "[TTS]$translatedMsg" else translatedMsg)
                
                if (currentTtl > 0) {
                    val newTtl = currentTtl - 1
                    delay(500)
                    // preserve original payload but decrement TTL
                    val prioTag = if (msgPriority != "YELLOW") "[PRIO:$msgPriority]" else ""
                    broadcastMessage("[TTL:$newTtl][ID:$msgId]$prioTag$actualMessageInner")
                }
                
                if (actualMessageInner.contains("SOS", ignoreCase = true) || actualMessageInner.contains("DEATH RATTLE", ignoreCase = true)) {
                    com.example.itantra.hardware.FlashlightManager.strobeSos(context)
                }"""

replace = """                var msgId = ""
                var msgPriority = "YELLOW"
                var msgLang = "English"
                var inner = plainText
                
                while (inner.startsWith("[")) {
                    if (inner.startsWith("[ID:")) {
                        val end = inner.indexOf("]")
                        if (end != -1) {
                            msgId = inner.substring(4, end)
                            inner = inner.substring(end + 1)
                            if (triageRepo.victimExists(msgId)) {
                                return@launch
                            }
                            broadcastMessage("[ACK:$msgId]")
                            continue
                        }
                    }
                    if (inner.startsWith("[PRIO:")) {
                        val end = inner.indexOf("]")
                        if (end != -1) {
                            msgPriority = inner.substring(6, end)
                            inner = inner.substring(end + 1)
                            continue
                        }
                    }
                    if (inner.startsWith("[LANG:")) {
                        val end = inner.indexOf("]")
                        if (end != -1) {
                            msgLang = inner.substring(6, end)
                            inner = inner.substring(end + 1)
                            continue
                        }
                    }
                    break
                }
                
                if (inner.startsWith("[ACK:")) {
                    val ackedId = inner.substringAfter("[ACK:").substringBefore("]")
                    triageRepo.markAsAcked(ackedId)
                    return@launch
                }
                
                var displayMsg = inner
                if (inner.startsWith("[TTS]")) {
                    displayMsg = "[TTS][$msgLang]" + inner.removePrefix("[TTS]")
                }
                
                val entity = TriageEntity(id = if(msgId.isNotEmpty()) msgId else java.util.UUID.randomUUID().toString(), message = inner.removePrefix("[TTS]"), priority = msgPriority, isSentByMe = false)
                triageRepo.insertVictim(entity)
                _incomingMessages.tryEmit(displayMsg)
                
                if (currentTtl > 0) {
                    val newTtl = currentTtl - 1
                    delay(500)
                    val prioTag = if (msgPriority != "YELLOW") "[PRIO:$msgPriority]" else ""
                    val langTag = if (msgLang != "English") "[LANG:$msgLang]" else ""
                    broadcastMessage("[TTL:$newTtl]$langTag[ID:$msgId]$prioTag$inner")
                }
                
                if (inner.contains("SOS", ignoreCase = true) || inner.contains("DEATH RATTLE", ignoreCase = true)) {
                    com.example.itantra.hardware.FlashlightManager.strobeSos(context)
                }"""
code = code.replace(search, replace)

# Add sendWithRetry before broadcastMessage
code = code.replace("    fun broadcastMessage(message: String) {", """    fun sendWithRetry(msgId: String, message: String) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            var retries = 0
            while (retries < 4) {
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
