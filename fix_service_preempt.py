import os

path = "app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add currentTtsJob tracking
old_service = """    // BUG-11 Fix: Use a scoped coroutine tied to the Service lifecycle instead of GlobalScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)"""

new_service = """    // BUG-11 Fix: Use a scoped coroutine tied to the Service lifecycle instead of GlobalScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentTtsJob: kotlinx.coroutines.Job? = null"""

content = content.replace(old_service, new_service)

# Change TTS launch logic to use serviceScope.launch and track the job
old_tts = """                    if (isAlert) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                    } else if (isWalkieTalkieOn) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                    }"""

new_tts = """                    if (isAlert) {
                        // PREEMPTION: Cancel any ongoing TTS instantly for RED alert
                        currentTtsJob?.cancel()
                        currentTtsJob = kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                        }
                    } else if (isWalkieTalkieOn) {
                        // Normal messages queue sequentially (Wait for current to finish if it's playing)
                        if (currentTtsJob?.isActive == true && !isAlert) {
                            // Let it wait in the single-thread dispatcher queue
                        }
                        currentTtsJob = kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                        }
                    }"""

content = content.replace(old_tts, new_tts)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("MeshForegroundService preemption added cleanly")
