package com.example.itantra.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.io.FileOutputStream
import android.speech.tts.TextToSpeech
import java.util.Locale

object TTSEngine {
    private const val TAG = "TTSEngine"
    private var tts: OfflineTts? = null
    private var currentLang: String = ""
    
    // Fallback Android TTS
    private var androidTts: TextToSpeech? = null
    private var isAndroidTtsReady = false

    suspend fun init(context: Context, language: String = "English") = withContext(Dispatchers.IO) {
        if (tts != null && currentLang == language) return@withContext
        
        Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")
        
        // Setup Android TTS Fallback just in case
        if (androidTts == null) {
            androidTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isAndroidTtsReady = true
                }
            }
        }
        
        try {
            tts?.release() // Release old model
            tts = null
            
            var modelDir = "vits-piper-en_US-amy-low"
            var onnxFile = "en_US-amy-low.onnx"
            
            if (language == "Hindi") {
                modelDir = "vits-piper-hi_IN-rohan-medium"
                onnxFile = "hi_IN-rohan-medium.onnx"
            } else if (language != "English") {
                // For other languages, we rely entirely on Android TTS
                currentLang = language
                return@withContext
            }

            val espeakDir = File(context.filesDir, "$modelDir/espeak-ng-data")
            val successFlag = File(espeakDir, ".success")
            if (!espeakDir.exists() || !successFlag.exists()) {
                Log.d(TAG, "Copying espeak-ng-data to filesDir...")
                if (espeakDir.exists()) espeakDir.deleteRecursively()
                copyDataDir(context, "$modelDir/espeak-ng-data", espeakDir)
                successFlag.createNewFile()
            }

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/$onnxFile",
                        tokens = "$modelDir/tokens.txt",
                        dataDir = espeakDir.absolutePath
                    ),
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                ),
                ruleFsts = "",
                maxNumSentences = 1
            )
            tts = OfflineTts(assetManager = context.assets, config = config)
            currentLang = language
            Log.d(TAG, "Sherpa-ONNX TTS initialized for $language.")
        } catch (e: Throwable) {
            Log.e(TAG, "Sherpa TTS Init Failed", e)
        }
    }

    private fun copyDataDir(context: Context, srcPath: String, destDir: File) {
        val assets = context.assets.list(srcPath) ?: return
        if (assets.isEmpty()) {
            destDir.parentFile?.mkdirs()
            context.assets.open(srcPath).use { input ->
                FileOutputStream(destDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            destDir.mkdirs()
            for (asset in assets) {
                val subSrc = "$srcPath/$asset"
                val subDest = File(destDir, asset)
                copyDataDir(context, subSrc, subDest)
            }
        }
    }

    suspend fun synthesizeAndPlay(text: String, context: Context? = null) = withContext(Dispatchers.IO) {
        if (currentLang != "English" && currentLang != "Hindi" && isAndroidTtsReady && androidTts != null) {
            // Fallback to Android TTS for other languages
            Log.d(TAG, "Falling back to Android TTS for $currentLang")
            val loc = when(currentLang) {
                "Marathi" -> Locale("mr", "IN")
                "Tamil" -> Locale("ta", "IN")
                "Bengali" -> Locale("bn", "IN")
                "Telugu" -> Locale("te", "IN")
                "Gujarati" -> Locale("gu", "IN")
                "Kannada" -> Locale("kn", "IN")
                "Malayalam" -> Locale("ml", "IN")
                "Odia" -> Locale("or", "IN")
                "Urdu" -> Locale("ur", "IN")
                else -> Locale.ENGLISH
            }
            androidTts!!.language = loc
            androidTts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            return@withContext
        }

        if (tts == null) {
            if (context != null) init(context, currentLang)
            if (tts == null) return@withContext
        }
        
        Log.d(TAG, "Sherpa TTS generating: $text")
        try {
            val audio = tts!!.generate(text)
            if (audio.samples.isEmpty()) return@withContext
            
            val shortArray = ShortArray(audio.samples.size) { i ->
                (audio.samples[i] * 32767).toInt().toShort()
            }
            
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                audio.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                shortArray.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(shortArray, 0, shortArray.size)
            audioTrack.play()
            // Bug 4 Fix: Release AudioTrack to prevent audio resource exhaustion
            val durationMs = (audio.samples.size.toLong() * 1000L) / audio.sampleRate.toLong() + 300L
            Thread.sleep(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Throwable) {
            Log.e(TAG, "TTS Generation/Playback failed", e)
        }
    }


    suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(Dispatchers.IO) {
        if (tts == null) {
            if (context != null) init(context, if (currentLang.isEmpty()) "English" else currentLang)
            if (tts == null) return@withContext Pair(0L, 0f)
        }
        Log.d(TAG, "Sherpa TTS Benchmarking: $text")
        try {
            val startTime = System.currentTimeMillis()
            val audio = tts!!.generate(text)
            val endTime = System.currentTimeMillis()
            
            if (audio.samples.isEmpty()) return@withContext Pair(0L, 0f)
            
            val inferenceTimeMs = endTime - startTime
            val audioDurationMs = (audio.samples.size.toFloat() / audio.sampleRate.toFloat()) * 1000f
            
            val shortArray = ShortArray(audio.samples.size) { i ->
                (audio.samples[i] * 32767).toInt().toShort()
            }
            
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                audio.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                shortArray.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(shortArray, 0, shortArray.size)
            audioTrack.play()
            val durationMs2 = (audio.samples.size.toLong() * 1000L) / audio.sampleRate.toLong() + 300L
            Thread.sleep(durationMs2)
            audioTrack.stop()
            audioTrack.release()
            
            return@withContext Pair(inferenceTimeMs, audioDurationMs)
        } catch (e: Throwable) {
            Log.e(TAG, "TTS Benchmark failed", e)
            return@withContext Pair(0L, 0f)
        }
    }

    fun stop() {
        Log.d(TAG, "Sherpa TTS stopped.")
        androidTts?.stop()
    }

    fun shutdown() {
        tts?.release()
        tts = null
        currentLang = ""
        Log.d(TAG, "Sherpa TTS shutdown completely.")
    }
}
