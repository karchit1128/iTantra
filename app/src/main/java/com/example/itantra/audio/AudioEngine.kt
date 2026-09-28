package com.example.itantra.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine {

    companion object {
        private const val TAG = "iTantraAudioEngine"
        // Strict 16kHz required by AI4Bharat models
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // VAD (Voice Activity Detection) Parameters
        private const val SILENCE_THRESHOLD = 2000 // Amplitude below this is considered silence
        private const val VAD_PAUSE_MS = 500L // Trigger STT after 1 second of silence
        
        // Goldfish Memory Buffer (Prevents Out-Of-Memory exceptions)
        private const val MAX_SECONDS = 30 // Up to 30s per PTT message0 // Up to 30s per PTT message
        private const val MAX_SAMPLES = SAMPLE_RATE * MAX_SECONDS
    }

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    
    // Circular Buffer for Goldfish Memory
    private var audioBuffer = ShortArray(MAX_SAMPLES)
    private var bufferHead = 0
    private var isBufferFull = false
    
    // VAD State
    private var lastVoiceDetectedTime = 0L
    private var onCompleteCallback: ((ShortArray) -> Unit)? = null
    private var disableVad = false

    @SuppressLint("MissingPermission") // We request permission via Compose UI before calling this
    fun startRecording(disableVad: Boolean = false, onComplete: ((ShortArray) -> Unit)? = null) {
        if (isRecording.get()) return
        
        this.onCompleteCallback = onComplete
        this.disableVad = disableVad
        isRecording.set(true)
        Log.d(TAG, "Starting audio engine asynchronously...")

        recordingThread = Thread {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                
                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord buffer size error")
                    stopRecording(null)
                    return@Thread
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize * 4 // Double buffer for safety
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize")
                    stopRecording(null)
                    return@Thread
                }
                
                // Reset buffer
                bufferHead = 0
                isBufferFull = false
                lastVoiceDetectedTime = System.currentTimeMillis()

                val sessionId = audioRecord?.audioSessionId ?: -1
                if (sessionId != -1 && android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    val ns = android.media.audiofx.NoiseSuppressor.create(sessionId)
                    ns?.enabled = true
                    Log.d(TAG, "Hardware NoiseSuppressor Enabled.")
                }

                audioRecord?.startRecording()
                Log.d(TAG, "Started recording 16kHz audio with Goldfish Memory and VAD...")

                captureLoop(minBufferSize)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio engine: ${e.message}")
                stopRecording(null)
            }
        }.also { it.start() }
    }

    private fun captureLoop(bufferSize: Int) {
        val audioData = ShortArray(bufferSize)
        while (isRecording.get()) {
            val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: 0
            if (bytesRead > 0) {
                // Amplitude tracking for VAD
                val maxAmplitude = audioData.take(bytesRead).maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                val now = System.currentTimeMillis()
                
                if (!disableVad) {
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
                }
                
                // Write into Goldfish Circular Buffer
                for (i in 0 until bytesRead) {
                    audioBuffer[bufferHead] = audioData[i]
                    bufferHead++
                    if (bufferHead >= MAX_SAMPLES) {
                        bufferHead = 0
                        isBufferFull = true
                    }
                }
            }
        }
    }

    /**
     * Stops recording and invokes the callback with the full accumulated 16kHz PCM audio buffer.
     * Can be called manually by UI or automatically by VAD.
     */
    fun stopRecording(manualCallbackOverride: ((ShortArray) -> Unit)? = null) {
        if (!isRecording.getAndSet(false)) return

        Log.d(TAG, "Stopping audio engine...")
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            recordingThread = null
            
            // Extract the Goldfish Memory buffer linearly
            val resultSize = if (isBufferFull) MAX_SAMPLES else bufferHead
            val finalAudio = ShortArray(resultSize)
            
            if (isBufferFull) {
                // Copy from head to end, then 0 to head
                System.arraycopy(audioBuffer, bufferHead, finalAudio, 0, MAX_SAMPLES - bufferHead)
                System.arraycopy(audioBuffer, 0, finalAudio, MAX_SAMPLES - bufferHead, bufferHead)
            } else {
                System.arraycopy(audioBuffer, 0, finalAudio, 0, bufferHead)
            }
            
            Log.d(TAG, "Audio extracted from Goldfish buffer. Length: ${finalAudio.size} samples")
            
            val targetCallback = manualCallbackOverride ?: onCompleteCallback
            
            targetCallback?.invoke(finalAudio)
            
            onCompleteCallback = null // clear reference
        }
    }
}
