with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

search = """            if (isAlert) {
                val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let {
                    val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                }
            }
            audioTrack.write(shortArray, 0, shortArray.size)
            audioTrack.play()"""
replace = """            if (isAlert) {
                val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let {
                    val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                }
                
                // Play Siren first
                try {
                    val toneGen = android.media.ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
                    Thread.sleep(1500)
                    toneGen.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Siren failed", e)
                }
                
                // Increase Pitch/Speed for RED Alert
                try {
                    val defaultRate = audioTrack.playbackRate
                    audioTrack.playbackRate = (defaultRate * 1.3f).toInt()
                } catch (e: Exception) {
                    Log.e(TAG, "Playback rate change failed", e)
                }
            }
            audioTrack.write(shortArray, 0, shortArray.size)
            audioTrack.play()"""
code = code.replace(search, replace)
with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
