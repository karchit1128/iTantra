import re

path = "app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add a handsFreeState
old_state = """    var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }"""
new_state = """    var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }
    var isHandsFreeMode by remember { mutableStateOf(false) }"""
content = content.replace(old_state, new_state)

# Replace the text "Hold mic to speak \xB7 type to chat" to include a toggle for Hands-Free mode
old_text = """            Text("Hold mic to speak \u00B7 type to chat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)"""
new_text = """            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(if (isHandsFreeMode) "Hands-Free VAD Active \u00B7 Tap to stop" else "Hold mic to speak \u00B7 type to chat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(16.dp))
                Text("VAD", fontSize = 10.sp, color = TextSecondary)
                Switch(
                    checked = isHandsFreeMode,
                    onCheckedChange = { isHandsFreeMode = it },
                    modifier = Modifier.scale(0.6f)
                )
            }"""
content = content.replace(old_text, new_text)

# Change hardware PTT to respect the toggle
old_hw = """            audioEngine.startRecording(disableVad = true) { audioData ->"""
new_hw = """            audioEngine.startRecording(disableVad = !isHandsFreeMode) { audioData ->"""
content = content.replace(old_hw, new_hw)

# For the software PTT, if it's Hands-Free mode, tap to start, tap to stop. If not, hold to talk.
old_ptr = """.pointerInput(isPttDisabled) {
                                if (isPttDisabled) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!permissionsGranted) { 
                                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                                        return@awaitEachGesture
                                    }
                                    isRecording = true
                                    meshManager.lockChannel(myDeviceId)
                                    audioEngine.startRecording(disableVad = !isHandsFreeMode) { audioData ->
                                        coroutineScope.launch {
                                            meshManager.unlockChannel()
                                            val transcript = if (audioData.isNotEmpty()) com.example.itantra.ml.STTEngine.transcribe(audioData, selectedLanguage) else com.example.itantra.ml.SttResult.Empty()
                                            isRecording = false // Set idle only after STT completes, not before
                                            when (transcript) {
                                                is com.example.itantra.ml.SttResult.Success -> {
                                                    if (showRawStt) lastSttDebugInfo = "PTT RAW:\\n${transcript.rawText}\\nGATE:\\n${transcript.text}"
                                                    val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                                                    val lower = transcript.text.lowercase()
                                                    val prio = selectedPriority // BUG-22 Fix: Trust the user's UI selection instead of English-only keywords
                                                    val msgId = java.util.UUID.randomUUID().toString()
                                                    val entity = TriageEntity(id = msgId, message = transcript.text, priority = prio, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                                                    triageRepo.insertVictim(entity)
                                                    meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]${transcript.text}")
                                                }
                                                is com.example.itantra.ml.SttResult.LangMismatch -> {
                                                    showLangMismatchDialog = transcript
                                                }
                                                is com.example.itantra.ml.SttResult.Empty -> {
                                                    // Do nothing
                                                }
                                                is com.example.itantra.ml.SttResult.Failed -> {
                                                    android.util.Log.e("WalkieTalkie", "STT failed: ${transcript.error}")
                                                }
                                            }
                                        }
                                    }
                                    waitForUpOrCancellation()
                                    audioEngine.stopRecording()
                                }
                            }"""

new_ptr = """.pointerInput(isPttDisabled, isHandsFreeMode) {
                                if (isPttDisabled) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!permissionsGranted) { 
                                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                                        return@awaitEachGesture
                                    }
                                    
                                    if (isHandsFreeMode) {
                                        if (isRecording) {
                                            audioEngine.stopRecording()
                                        } else {
                                            isRecording = true
                                            meshManager.lockChannel(myDeviceId)
                                            audioEngine.startRecording(disableVad = false) { audioData ->
                                                coroutineScope.launch {
                                                    meshManager.unlockChannel()
                                                    val transcript = if (audioData.isNotEmpty()) com.example.itantra.ml.STTEngine.transcribe(audioData, selectedLanguage) else com.example.itantra.ml.SttResult.Empty()
                                                    isRecording = false
                                                    when (transcript) {
                                                        is com.example.itantra.ml.SttResult.Success -> {
                                                            if (showRawStt) lastSttDebugInfo = "PTT RAW:\\n${transcript.rawText}\\nGATE:\\n${transcript.text}"
                                                            val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                                                            val prio = selectedPriority
                                                            val msgId = java.util.UUID.randomUUID().toString()
                                                            val entity = TriageEntity(id = msgId, message = transcript.text, priority = prio, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                                                            triageRepo.insertVictim(entity)
                                                            meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]${transcript.text}")
                                                        }
                                                        is com.example.itantra.ml.SttResult.LangMismatch -> {
                                                            showLangMismatchDialog = transcript
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }
                                        }
                                        waitForUpOrCancellation() // Consume up
                                    } else {
                                        // PTT Mode
                                        isRecording = true
                                        meshManager.lockChannel(myDeviceId)
                                        audioEngine.startRecording(disableVad = true) { audioData ->
                                            coroutineScope.launch {
                                                meshManager.unlockChannel()
                                                val transcript = if (audioData.isNotEmpty()) com.example.itantra.ml.STTEngine.transcribe(audioData, selectedLanguage) else com.example.itantra.ml.SttResult.Empty()
                                                isRecording = false
                                                when (transcript) {
                                                    is com.example.itantra.ml.SttResult.Success -> {
                                                        if (showRawStt) lastSttDebugInfo = "PTT RAW:\\n${transcript.rawText}\\nGATE:\\n${transcript.text}"
                                                        val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                                                        val prio = selectedPriority
                                                        val msgId = java.util.UUID.randomUUID().toString()
                                                        val entity = TriageEntity(id = msgId, message = transcript.text, priority = prio, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                                                        triageRepo.insertVictim(entity)
                                                        meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]${transcript.text}")
                                                    }
                                                    is com.example.itantra.ml.SttResult.LangMismatch -> {
                                                        showLangMismatchDialog = transcript
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                        waitForUpOrCancellation()
                                        audioEngine.stopRecording()
                                    }
                                }
                            }"""

content = content.replace(old_ptr, new_ptr)
with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("WalkieTalkieScreen updated with VAD Switch!")
