import os

path = "app/src/main/java/com/example/itantra/ml/TTSEngine.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add preemption state
old_header = """    private var tts: OfflineTts? = null
    var currentLang: String = ""
    var loadedLang: String = ""
        
    private val ttsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()"""
new_header = """    private var tts: OfflineTts? = null
    var currentLang: String = ""
    var loadedLang: String = ""
        
    private val ttsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    
    // Alert Preemption State
    @Volatile private var activeAudioTrack: AudioTrack? = null
    @Volatile private var isAlertPlaying = false"""
content = content.replace(old_header, new_header)

# Modify synthesizeAndPlay preemption logic
old_play_sig = """    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) {"""
new_play_sig = """    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) {
        if (isAlert) {
            // Preempt any currently playing normal message immediately
            try {
                activeAudioTrack?.stop()
                activeAudioTrack?.release()
                activeAudioTrack = null
            } catch (e: Exception) {}
            isAlertPlaying = true
        } else {
            // If an alert is currently playing, drop or queue normal messages
            // (Queueing happens naturally via ttsDispatcher, but we shouldn't play over an alert)
            if (isAlertPlaying) return@withContext
        }"""
content = content.replace(old_play_sig, new_play_sig)

# Track AudioTrack
old_track = """            val audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                shortArray.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )"""
new_track = """            val audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                shortArray.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            activeAudioTrack = audioTrack"""
content = content.replace(old_track, new_track)

# Reset state after play
old_end = """            delay(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Throwable) {
            Log.e(TAG, "TTS Generation/Playback failed", e)
        }
    }"""
new_end = """            delay(durationMs)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch(e: Exception) {}
            
            if (activeAudioTrack == audioTrack) {
                activeAudioTrack = null
                if (isAlert) isAlertPlaying = false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "TTS Generation/Playback failed", e)
            if (isAlert) isAlertPlaying = false
        }
    }"""
content = content.replace(old_end, new_end)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("TTSEngine preemption added")
