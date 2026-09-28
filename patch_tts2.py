import re

with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

if "java.util.concurrent.Executors" not in code:
    code = code.replace("import kotlinx.coroutines.sync.withLock", "import kotlinx.coroutines.sync.withLock\nimport java.util.concurrent.Executors\nimport kotlinx.coroutines.asCoroutineDispatcher")

if "ttsDispatcher =" not in code:
    code = code.replace("private val mutex = Mutex()", "private val mutex = Mutex()\n    private val ttsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()")

code = code.replace("suspend fun init(context: Context, language: String = \"English\") = withContext(Dispatchers.IO) {", "suspend fun init(context: Context, language: String = \"English\") = withContext(ttsDispatcher) {")
code = code.replace("suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(Dispatchers.IO) {", "suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) {")
code = code.replace("suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(Dispatchers.IO) {", "suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(ttsDispatcher) {")

espeak_search = r'dataDir = "\$modelDir/espeak-ng-data"'
espeak_replace = r'dataDir = File(context.filesDir, "espeak-ng-data").absolutePath'
code = re.sub(espeak_search, espeak_replace, code)

copy_logic = """
            val sharedEspeakDir = File(context.filesDir, "espeak-ng-data")
            if (!sharedEspeakDir.exists() || sharedEspeakDir.list()?.isEmpty() == true) {
                Log.d(TAG, "Copying shared espeak-ng-data to filesDir...")
                copyDataDir(context, "vits-piper-hi_IN-rohan-medium/espeak-ng-data", sharedEspeakDir)
            }
"""
if "sharedEspeakDir =" not in code:
    code = code.replace("tts?.release() // Release old model\n            tts = null", "tts?.release() // Release old model\n            tts = null\n" + copy_logic)

# For shutdown, we want it blocking or suspend? shutdown is currently just `fun shutdown()`. Let's wrap it in GlobalScope if it's not suspend, but wait, shutdown must be suspend to use ttsDispatcher cleanly, or we can just use runBlocking. Let's make it suspend.
code = code.replace("fun shutdown() {", "suspend fun shutdown() = withContext(ttsDispatcher) { mutex.withLock {")
# But then we need to add the closing brace.
code = code.replace("Log.d(TAG, \"Sherpa TTS shutdown completely.\")\n    }", "Log.d(TAG, \"Sherpa TTS shutdown completely.\")\n    } }")

# Add mutex.withLock to synthesizeAndPlay
# Because of early returns, wrapping the whole body is tricky with string replacement.
# Instead, let's just use regular replacement for the whole body of synthesizeAndPlay and benchmarkTTS.
with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
