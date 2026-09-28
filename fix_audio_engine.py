import re

path = "app/src/main/java/com/example/itantra/audio/AudioEngine.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add loopStartTime
old_loop = """        val audioData = ShortArray(bufferSize)
        while (isRecording.get()) {"""
new_loop = """        val audioData = ShortArray(bufferSize)
        val loopStartTime = System.currentTimeMillis()
        while (isRecording.get()) {"""
content = content.replace(old_loop, new_loop)

# 2. Add 15s cap to VAD logic
old_vad = """                if (!disableVad) {
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
                }"""
new_vad = """                if (!disableVad) {
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
content = content.replace(old_vad, new_vad)

# 3. Synchronize buffer writes
old_write = """                // Write into Goldfish Circular Buffer
                for (i in 0 until bytesRead) {
                    audioBuffer[bufferHead] = audioData[i]
                    bufferHead++
                    if (bufferHead >= MAX_SAMPLES) {
                        bufferHead = 0
                        isBufferFull = true
                    }
                }"""
new_write = """                // BUG-20 Fix: Synchronize buffer writes
                synchronized(audioBuffer) {
                    for (i in 0 until bytesRead) {
                        audioBuffer[bufferHead] = audioData[i]
                        bufferHead++
                        if (bufferHead >= MAX_SAMPLES) {
                            bufferHead = 0
                            isBufferFull = true
                        }
                    }
                }"""
content = content.replace(old_write, new_write)

# 4. Synchronize extraction
old_extract = """            // Extract the Goldfish Memory buffer linearly
            val resultSize = if (isBufferFull) MAX_SAMPLES else bufferHead
            val finalAudio = ShortArray(resultSize)
            
            if (isBufferFull) {
                // Copy from head to end, then 0 to head
                System.arraycopy(audioBuffer, bufferHead, finalAudio, 0, MAX_SAMPLES - bufferHead)
                System.arraycopy(audioBuffer, 0, finalAudio, MAX_SAMPLES - bufferHead, bufferHead)
            } else {
                System.arraycopy(audioBuffer, 0, finalAudio, 0, bufferHead)
            }"""
new_extract = """            // BUG-20 Fix: Synchronize extraction
            val finalAudio: ShortArray
            synchronized(audioBuffer) {
                val resultSize = if (isBufferFull) MAX_SAMPLES else bufferHead
                finalAudio = ShortArray(resultSize)
                
                if (isBufferFull) {
                    // Copy from head to end, then 0 to head
                    System.arraycopy(audioBuffer, bufferHead, finalAudio, 0, MAX_SAMPLES - bufferHead)
                    System.arraycopy(audioBuffer, 0, finalAudio, MAX_SAMPLES - bufferHead, bufferHead)
                } else {
                    System.arraycopy(audioBuffer, 0, finalAudio, 0, bufferHead)
                }
            }"""
content = content.replace(old_extract, new_extract)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("AudioEngine.kt patched successfully")
