import os
import re

def patch_file(filepath, patches):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    for old, new in patches:
        if old not in content:
            print(f"WARNING: Chunk not found in {filepath}:\n{old[:50]}...")
        else:
            content = content.replace(old, new)
            
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Patched {filepath}")

# BUG-19: JuryEvaluationScreen shutdown() removal
patch_file("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", [
    (
        "                        } finally {\n                            TTSEngine.shutdown()\n                        }",
        "                        } finally {\n                            // BUG-19 Fix: No shutdown to preserve espeak state\n                        }"
    )
])

# BUG-12: MeshNetworkManager sendWithRetry check
patch_file("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", [
    (
        "        scope.launch {\n            val startTime = System.currentTimeMillis()\n            var retries = 0\n            while (retries < 4) {\n                broadcastMessage(message)\n                delay(2000)\n                if (triageRepo.isAcked(msgId)) {",
        "        scope.launch {\n            // BUG-12 Fix: Exit early if already acked\n            if (triageRepo.isAcked(msgId)) return@launch\n            val startTime = System.currentTimeMillis()\n            var retries = 0\n            while (retries < 4) {\n                broadcastMessage(message)\n                delay(2000)\n                if (triageRepo.isAcked(msgId)) {"
    )
])

# BUG-21: BluetoothFallbackManager buffered read
patch_file("app/src/main/java/com/example/itantra/mesh/BluetoothFallbackManager.kt", [
    (
        "                val inputStream: InputStream = socket.inputStream\n                val buffer = ByteArray(4096)\n                while (true) {\n                    val bytes = inputStream.read(buffer)\n                    if (bytes > 0) {\n                        val encryptedMessage = String(buffer, 0, bytes)",
        "                val dataIn = java.io.DataInputStream(socket.inputStream)\n                while (true) {\n                    val bytes = dataIn.readInt()\n                    if (bytes > 0) {\n                        val buffer = ByteArray(bytes)\n                        dataIn.readFully(buffer)\n                        val encryptedMessage = String(buffer)"
    ),
    (
        "                val outStream: OutputStream = socket.outputStream\n                outStream.write(bytes)",
        "                val dataOut = java.io.DataOutputStream(socket.outputStream)\n                dataOut.writeInt(bytes.size)\n                dataOut.write(bytes)\n                dataOut.flush()"
    )
])

# BUG-15 & BUG-20: AudioEngine 15s cap & synchronized buffer
audio_engine_path = "app/src/main/java/com/example/itantra/audio/AudioEngine.kt"
with open(audio_engine_path, "r", encoding="utf-8") as f:
    audio_content = f.read()

# BUG-20: Synchronize captureLoop
audio_content = audio_content.replace(
    "                // Write into Goldfish Circular Buffer\n                for (i in 0 until bytesRead) {\n                    audioBuffer[bufferHead] = audioData[i]\n                    bufferHead++\n                    if (bufferHead >= MAX_SAMPLES) {\n                        bufferHead = 0\n                        isBufferFull = true\n                    }\n                }",
    "                // BUG-20 Fix: Synchronize buffer writes\n                synchronized(audioBuffer) {\n                    for (i in 0 until bytesRead) {\n                        audioBuffer[bufferHead] = audioData[i]\n                        bufferHead++\n                        if (bufferHead >= MAX_SAMPLES) {\n                            bufferHead = 0\n                            isBufferFull = true\n                        }\n                    }\n                }"
)

# BUG-20: Synchronize stopRecording extract
audio_content = audio_content.replace(
    "            val resultSize = if (isBufferFull) MAX_SAMPLES else bufferHead\n            val finalAudio = ShortArray(resultSize)\n            \n            if (isBufferFull) {\n                // Copy from head to end, then 0 to head\n                System.arraycopy(audioBuffer, bufferHead, finalAudio, 0, MAX_SAMPLES - bufferHead)\n                System.arraycopy(audioBuffer, 0, finalAudio, MAX_SAMPLES - bufferHead, bufferHead)\n            } else {\n                System.arraycopy(audioBuffer, 0, finalAudio, 0, bufferHead)\n            }",
    "            val finalAudio: ShortArray\n            synchronized(audioBuffer) {\n                val resultSize = if (isBufferFull) MAX_SAMPLES else bufferHead\n                finalAudio = ShortArray(resultSize)\n                if (isBufferFull) {\n                    System.arraycopy(audioBuffer, bufferHead, finalAudio, 0, MAX_SAMPLES - bufferHead)\n                    System.arraycopy(audioBuffer, 0, finalAudio, MAX_SAMPLES - bufferHead, bufferHead)\n                } else {\n                    System.arraycopy(audioBuffer, 0, finalAudio, 0, bufferHead)\n                }\n            }"
)

# BUG-15: Max duration cap for PTT
audio_content = audio_content.replace(
    "        val audioData = ShortArray(bufferSize)\n        while (isRecording.get()) {",
    "        val audioData = ShortArray(bufferSize)\n        val loopStartTime = System.currentTimeMillis()\n        while (isRecording.get()) {"
)

cap_fix = """                if (!disableVad) {
                    if (maxAmplitude > SILENCE_THRESHOLD) {
                        lastVoiceDetectedTime = now
                        Log.d(TAG, "Voice Detected! Amplitude: $maxAmplitude")
                    } else {
                        // Check if we hit the silence pause limit
                        if (now - lastVoiceDetectedTime > VAD_PAUSE_MS) {
                            Log.d(TAG, "VAD detected $VAD_PAUSE_MS ms of silence. Auto-triggering stop.")
                            // Automatically stop and trigger transcription
                            stopRecording(null)
                            break
                        }
                    }
                } else {
                    // BUG-15 Fix: Hard cap at 15s when VAD is disabled (PTT mode)
                    if (now - loopStartTime > 15000) {
                        Log.w(TAG, "PTT max duration reached (15s). Auto-stopping.")
                        stopRecording(null)
                        break
                    }
                }"""

audio_content = re.sub(
    r"                if \(\!disableVad\) \{.*?\}",
    cap_fix,
    audio_content,
    flags=re.DOTALL
)

with open(audio_engine_path, "w", encoding="utf-8") as f:
    f.write(audio_content)
print(f"Patched {audio_engine_path}")

