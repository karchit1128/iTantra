package com.example.itantra.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.k2fsa.sherpa.onnx.*

object STTEngine {
    private const val TAG = "STTEngine"
    var isInitialized = false
    
    private var recognizer: OfflineRecognizer? = null
    private var cacheDir: java.io.File? = null

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        if (!AssetValidator.preflightStt(context)) {
            Log.e(TAG, "Preflight failed for STT model. Skipping native init.")
            return@withContext
        }
        cacheDir = context.filesDir
        Log.d(TAG, "Sherpa-ONNX STT initializing (Dolphin Multilingual)...")
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    dolphin = OfflineDolphinModelConfig(
                        model = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/model.int8.onnx"
                    ),
                    tokens = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/tokens.txt",
                    numThreads = 1,
                    debug = false
                )
            )
            recognizer = OfflineRecognizer(assetManager = context.assets, config = config)
            isInitialized = true
            Log.d(TAG, "Sherpa-ONNX STT initialized (Dolphin Multilingual).")
        } catch (e: Throwable) {
            Log.e(TAG, "Sherpa STT Init Failed", e)
        }
    }

    suspend fun transcribe(audioData: ShortArray, targetLanguage: String = "English"): SttResult = withContext(Dispatchers.Default) {
        // BUG-8 Fix: If STT crashed (isInitialized=true but recognizer=null), reset so next call re-inits
        if (isInitialized && recognizer == null) {
            android.util.Log.w(TAG, "STT crash recovery: resetting isInitialized flag")
            isInitialized = false
        }
        if (!isInitialized || recognizer == null) return@withContext SttResult.Empty()
        
        Log.d(TAG, "Sherpa STT decoding ${audioData.size} samples for $targetLanguage...")
        var stream: OfflineStream? = null
        try {
            stream = recognizer!!.createStream()
            val GAIN = 1.0f // Reverted back to 1.0f to prevent heavy audio clipping/distortion
            val floatArray = FloatArray(audioData.size) { i -> 
                val floatVal = (audioData[i] / 32768.0f) * GAIN
                floatVal.coerceIn(-1.0f, 1.0f) 
            }
            stream.acceptWaveform(floatArray, 16000)
            recognizer!!.decode(stream)
            val result = recognizer!!.getResult(stream)
            var rawText = result.text; rawText = rawText.replace(Regex("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]"), "").trim()
            
            // STT Diagnostics (Mentor requested)
            val peak = if (audioData.isNotEmpty()) audioData.maxOf { kotlin.math.abs(it.toInt()) } else 0
            Log.d(TAG, "RAW='$rawText' samples=${audioData.size} peak=$peak")
            if (cacheDir != null) {
                java.io.File(cacheDir, "last_stt.wav").writeBytes(pcm16ToWav(audioData, 16000))
            }
            
            if (rawText.isBlank()) return@withContext SttResult.Empty(rawText)
            
            // BUG-3 Fix: LENIENT LANGUAGE GATE
            // Dolphin outputs romanized/mixed text. Only reject if >80% of alpha chars are wrong-script.
            val scriptRegex = when (targetLanguage) {
                "Hindi", "Marathi" -> Regex("[\u0900-\u097F]")
                "Gujarati" -> Regex("[\u0A80-\u0AFF]")
                "Bengali" -> Regex("[\u0980-\u09FF]")
                "Tamil" -> Regex("[\u0B80-\u0BFF]")
                "Telugu" -> Regex("[\u0C00-\u0C7F]")
                "Kannada" -> Regex("[\u0C80-\u0CFF]")
                "Malayalam" -> Regex("[\u0D00-\u0D7F]")
                "Odia" -> Regex("[\u0B00-\u0B7F]")
                "English" -> Regex("[a-zA-Z]")
                else -> Regex("[a-zA-Z]")
            }
            val allAlpha = rawText.filter { it.isLetter() }
            val targetScriptChars = scriptRegex.findAll(allAlpha).count()
            val mismatchRatio = if (allAlpha.isNotEmpty()) 1.0 - (targetScriptChars.toDouble() / allAlpha.length) else 0.0
            Log.d("STT_GATE", "Target: $targetLanguage | RAW: '$rawText' | Mismatch: $mismatchRatio")
            if (mismatchRatio > 0.8 && allAlpha.isNotEmpty()) {
                return@withContext SttResult.LangMismatch(rawText, "Language mismatch: resend or send as-is")
            }
            return@withContext SttResult.Success(rawText, rawText)
        } catch (e: Throwable) {
            Log.e(TAG, "STT Transcription failed", e)
            return@withContext SttResult.Failed(e.message ?: "Unknown error")
        } finally {
            stream?.release()
        }
    }

    fun shutdown() {
        recognizer?.release()
        isInitialized = false
        Log.d(TAG, "Sherpa STT shutdown.")
    }

    private fun pcm16ToWav(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val byteData = ByteArray(pcmData.size * 2)
        java.nio.ByteBuffer.wrap(byteData).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)
        
        val header = ByteArray(44)
        val totalDataLen = byteData.size + 36
        val byteRate = sampleRate * 2
        
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = 1.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2.toByte(); header[33] = 0
        header[34] = 16.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (byteData.size and 0xff).toByte()
        header[41] = ((byteData.size shr 8) and 0xff).toByte()
        header[42] = ((byteData.size shr 16) and 0xff).toByte()
        header[43] = ((byteData.size shr 24) and 0xff).toByte()
        
        return header + byteData
    }
}
