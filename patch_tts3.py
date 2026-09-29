import re

with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

# We need to wrap the contents of synthesizeAndPlay and benchmarkTTS with mutex.withLock { ... }
# Actually, since TTSEngine only calls generate() and init() which are blockingly executed on ttsDispatcher,
# it is safe. But let's wrap them carefully.
# Better to do it via a more robust script.

def wrap_with_lock(code, func_name):
    # Find the start of the function block
    pattern = rf"(suspend fun {func_name}.*?= withContext\(ttsDispatcher\) {{\n)(.*?)(^    }})"
    # We can't do this easily with simple regex due to nested braces.
    pass

# We will just replace `withContext(ttsDispatcher) {` with `withContext(ttsDispatcher) { mutex.withLock {`
# But we have early returns like `return@withContext`. We must change them to `return@withLock`.
code = code.replace("return@withContext", "return@withLock")
code = code.replace("suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) {", "suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(ttsDispatcher) { mutex.withLock {")
code = code.replace("suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(ttsDispatcher) {", "suspend fun benchmarkTTS(text: String, context: Context? = null): Pair<Long, Float> = withContext(ttsDispatcher) { mutex.withLock {")

# And add the closing brace to both functions. 
# synthesizeAndPlay ends at `Log.e(TAG, "TTS Generation/Playback failed", e)` \n `}`
code = code.replace("Log.e(TAG, \"TTS Generation/Playback failed\", e)\n        }\n    }", "Log.e(TAG, \"TTS Generation/Playback failed\", e)\n        }\n    } }")

# benchmarkTTS ends at `return@withLock Pair(0L, 0f)` \n `}`
code = code.replace("return@withLock Pair(0L, 0f)\n        }\n    }", "return@withLock Pair(0L, 0f)\n        }\n    } }")

with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
