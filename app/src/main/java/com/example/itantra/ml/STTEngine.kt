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

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
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

    suspend fun transcribe(audioData: ShortArray): String = withContext(Dispatchers.Default) {
        if (!isInitialized || recognizer == null) return@withContext ""
        
        Log.d(TAG, "Sherpa STT decoding ${audioData.size} samples...")
        try {
            val stream = recognizer!!.createStream()
            val floatArray = FloatArray(audioData.size) { i -> audioData[i] / 32768.0f }
            stream.acceptWaveform(floatArray, 16000)
            recognizer!!.decode(stream)
            val result = recognizer!!.getResult(stream)
            val text = result.text
            stream.release()
            return@withContext text
        } catch (e: Throwable) {
            Log.e(TAG, "STT Transcription failed", e)
            return@withContext ""
        }
    }

    fun shutdown() {
        recognizer?.release()
        isInitialized = false
        Log.d(TAG, "Sherpa STT shutdown.")
    }
}
