package com.example.itantra.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.media.AudioAttributes
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.io.FileOutputStream
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object TTSEngine {
    private const val TAG = "TTSEngine"
    private var tts: OfflineTts? = null
    var currentLang: String = ""
        private set
        
    private val mutex = Mutex()
    private val ttsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    
    // Fallback Android TTS
    private var androidTts: TextToSpeech? = null
    private var isAndroidTtsReady = false

    const val USE_SHARED_ESPEAK = true

    suspend fun init(context: Context, language: String = "English") = withContext(ttsDispatcher) {
        mutex.withLock {
            initLocked(context, language)
        }
    }

    private fun initLocked(context: Context, language: String = "English") {
        if (tts != null && currentLang == language) return
        
        val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
        if (language in sttOnlyLangs) {
            currentLang = language
            Log.d(TAG, "Skipping neural TTS initialization for $language (STT-only mode)")
            return
        }

        if (!AssetValidator.preflightTts(context, language)) {
            Log.e(TAG, "Preflight failed for $language. Skipping native init.")
            return
        }

        Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")
        
        if (androidTts == null) {
            androidTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val locale = when (language) {
                        "Hindi" -> Locale("hi", "IN")
                        "Marathi" -> Locale("mr", "IN")
                        "Telugu" -> Locale("te", "IN")
                        "Malayalam" -> Locale("ml", "IN")
                        else -> Locale.US
                    }
                    val result = androidTts?.setLanguage(locale)
                    isAndroidTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
                }
            }
        }

        try {
            tts?.release() // Release old model
            tts = null
            
            val (modelDir, onnxFile) = when (language) {
                "Hindi" -> Pair("vits-piper-hi_IN-rohan-medium", "hi_IN-rohan-medium.onnx")
                "Marathi" -> Pair("vits-piper-mr_IN-medium", "mr_IN-medium.onnx")
                "Telugu" -> Pair("vits-piper-te_IN-medium", "te_IN-medium.onnx")
                "Malayalam" -> Pair("vits-piper-ml_IN-medium", "ml_IN-medium.onnx")
                else -> Pair("vits-piper-en_US-amy-low", "en_US-amy-low.onnx")
            }

            var activeEspeakDir = "$modelDir/espeak-ng-data"
            if (USE_SHARED_ESPEAK) {
                val sharedEspeakDir = File(context.filesDir, "espeak-ng-data")
                val tmpEspeakDir = File(context.filesDir, "espeak-ng-data.tmp")
                val markerFile = File(context.filesDir, "espeak-ng-data.ok")
                
                if (!markerFile.exists()) {
                    Log.d(TAG, "Copying shared espeak-ng-data...")
                    if (tmpEspeakDir.exists()) tmpEspeakDir.deleteRecursively()
                    tmpEspeakDir.mkdirs()
                    // Copy from the English base as it contains all dicts in our merged setup
                    copyDataDir(context, "vits-piper-en_US-amy-low/espeak-ng-data", tmpEspeakDir)
                    
                    if (sharedEspeakDir.exists()) sharedEspeakDir.deleteRecursively()
                    tmpEspeakDir.renameTo(sharedEspeakDir)
                    markerFile.createNewFile()
                }
                
                if (markerFile.exists() && sharedEspeakDir.exists()) {
                    activeEspeakDir = sharedEspeakDir.absolutePath
                } else {
                    Log.e(TAG, "Shared espeak copy failed. Falling back to asset folder.")
                }
            }

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/$onnxFile",
                        tokens = "$modelDir/tokens.txt",
                        dataDir = activeEspeakDir
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

    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) {
        mutex.withLock {
            val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
            if (currentLang in sttOnlyLangs) {
                Log.d(TAG, "Skipping TTS generation for $currentLang (Enforcing STT-only mode)")
                return@withLock
            }
            
            if (tts == null) {
                if (context != null) initLocked(context, currentLang)
                if (tts == null) return@withLock
            }
            
            Log.d(TAG, "Sherpa TTS generating: $text")
            try {
                val audio = tts!!.generate(text)
                if (audio.samples.isEmpty()) return@withLock
                val peak = audio.samples.maxOfOrNull { kotlin.math.abs(it) } ?: 1.0f
                val scale = if (peak > 1.0f) 1.0f / peak else 1.0f
                
                val shortArray = ShortArray(audio.samples.size) { i ->
                    (audio.samples[i] * scale * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }
                
                val usage = if (isAlert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audio.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val audioTrack = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    shortArray.size * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                
                if (isAlert) {
                    val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.let {
                        val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                        it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                    }
                }
                audioTrack.write(shortArray, 0, shortArray.size)
                audioTrack.play()
                val durationMs = (audio.samples.size.toLong() * 1000L) / audio.sampleRate.toLong() + 300L
                Thread.sleep(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Throwable) {
                Log.e(TAG, "TTS Generation/Playback failed", e)
            }
        }
    }

    suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(ttsDispatcher) {
        mutex.withLock {
            val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
            if (currentLang in sttOnlyLangs) return@withLock Pair(0L, 0f)

            val isAlert = false
            if (tts == null) {
                if (context != null) initLocked(context, if (currentLang.isEmpty()) "English" else currentLang)
                if (tts == null) return@withLock Pair(0L, 0f)
            }
            Log.d(TAG, "Sherpa TTS Benchmarking: $text")
            try {
                val startTime = System.currentTimeMillis()
                val audio = tts!!.generate(text)
                val endTime = System.currentTimeMillis()
                
                if (audio.samples.isEmpty()) return@withLock Pair(0L, 0f)
                
                val inferenceTimeMs = endTime - startTime
                val audioDurationMs = (audio.samples.size.toFloat() / audio.sampleRate.toFloat()) * 1000f
                
                val peak = audio.samples.maxOfOrNull { kotlin.math.abs(it) } ?: 1.0f
                val scale = if (peak > 1.0f) 1.0f / peak else 1.0f
                
                val shortArray = ShortArray(audio.samples.size) { i ->
                    (audio.samples[i] * scale * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }
                
                val usage = if (isAlert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audio.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val audioTrack = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    shortArray.size * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                
                if (isAlert) {
                    val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.let {
                        val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                        it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                    }
                }
                audioTrack.write(shortArray, 0, shortArray.size)
                audioTrack.play()
                val durationMs2 = (audio.samples.size.toLong() * 1000L) / audio.sampleRate.toLong() + 300L
                Thread.sleep(durationMs2)
                audioTrack.stop()
                audioTrack.release()
                
                return@withLock Pair(inferenceTimeMs, audioDurationMs)
            } catch (e: Throwable) {
                Log.e(TAG, "TTS Benchmark failed", e)
                return@withLock Pair(0L, 0f)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Sherpa TTS stopped.")
        androidTts?.stop()
    }

    suspend fun shutdown() = withContext(ttsDispatcher) {
        mutex.withLock {
            tts?.release()
            tts = null
            currentLang = ""
            Log.d(TAG, "Sherpa TTS shutdown completely.")
        }
    }
}
