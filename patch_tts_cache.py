import re

filepath = 'app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Replace tts variable
code = code.replace("private var tts: OfflineTts? = null", "private val ttsCache = mutableMapOf<String, OfflineTts>()")

# 2. Remove safeReleaseTts function completely
code = re.sub(r'private suspend fun safeReleaseTts\(\) \{[\s\S]*?\}\s*\}', '', code)

# 3. Rewrite initLocked
initLocked_old = '''    private suspend fun initLocked(context: Context, language: String = "English") {
        if (tts != null && loadedLang == language) {
            currentLang = language
            return
        }
        
        val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
        if (language in sttOnlyLangs) {
            safeReleaseTts()
            currentLang = language
            Log.d(TAG, "Skipping neural TTS initialization for  (STT-only mode)")
            return
        }

        if (!AssetValidator.preflightTts(context, language)) {
            safeReleaseTts()
            currentLang = language
            Log.e(TAG, "Preflight failed for . Skipping native init.")
            return
        }

        Log.d(TAG, "Sherpa-ONNX TTS initializing for ...")

        try {
            safeReleaseTts()'''

initLocked_new = '''    private suspend fun initLocked(context: Context, language: String = "English") {
        if (ttsCache.containsKey(language)) {
            currentLang = language
            loadedLang = language
            return
        }
        
        val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
        if (language in sttOnlyLangs) {
            currentLang = language
            loadedLang = language
            Log.d(TAG, "Skipping neural TTS initialization for  (STT-only mode)")
            return
        }

        if (!AssetValidator.preflightTts(context, language)) {
            currentLang = language
            loadedLang = language
            Log.e(TAG, "Preflight failed for . Skipping native init.")
            return
        }

        Log.d(TAG, "Sherpa-ONNX TTS initializing for ...")

        try {'''
code = code.replace(initLocked_old, initLocked_new)

# 4. Replace tts assignment in initLocked
code = code.replace("tts = OfflineTts(assetManager = context.assets, config = config)", "val newTts = OfflineTts(assetManager = context.assets, config = config)\n            ttsCache[language] = newTts")

# 5. Fix synthesizeAndPlay
# Old: if (context != null && (tts == null || loadedLang != effectiveLang)) { ... }
# Old: if (tts == null) return@withContext
# Old: val audio = tts!!.generate(text)
code = code.replace("if (context != null && (tts == null || loadedLang != effectiveLang)) {", "if (context != null && !ttsCache.containsKey(effectiveLang)) {")
code = code.replace("if (tts == null) return@withContext", "val currentTts = ttsCache[effectiveLang]\n        if (currentTts == null) return@withContext")
code = code.replace("val audio = tts!!.generate(text)", "val audio = currentTts!!.generate(text)")

# 6. Fix benchmarkTTS
# Old: if (tts == null) { initLocked(context, currentLang) }
# Old: if (tts == null) return@withContext Pair(0L, 0f)
# Old: val audio = tts!!.generate(text)
code = code.replace("if (tts == null) {", "if (!ttsCache.containsKey(currentLang) && context != null) {")
code = code.replace("if (tts == null) return@withContext Pair(0L, 0f)", "val currentTts = ttsCache[currentLang]\n            if (currentTts == null) return@withContext Pair(0L, 0f)")

# 7. Fix shutdown
shutdown_old = '''    suspend fun shutdown() = withContext(ttsDispatcher) {
        safeReleaseTts()
        currentLang = ""
        Log.d(TAG, "Sherpa TTS shutdown completely.")
    }'''
shutdown_new = '''    suspend fun shutdown() = withContext(ttsDispatcher) {
        ttsCache.values.forEach { 
            try { kotlinx.coroutines.delay(100); it.release() } catch(e: Exception){} 
        }
        ttsCache.clear()
        currentLang = ""
        Log.d(TAG, "Sherpa TTS shutdown completely.")
    }'''
code = code.replace(shutdown_old, shutdown_new)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("TTS Cache Architectural Fix applied.")
