import re

filepath = 'app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Add safeReleaseTts function inside the object
safe_release_code = '''
    private suspend fun safeReleaseTts() {
        if (tts != null) {
            try {
                // TASK 1 Fix: Ensure native C++ background generation/teardown threads finish before destroying the mutex
                kotlinx.coroutines.delay(300)
                tts?.release()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Native TTS release race condition caught (mutex destroyed)", e)
            } finally {
                tts = null
            }
        }
    }
'''
code = code.replace("const val USE_SHARED_ESPEAK = true", "const val USE_SHARED_ESPEAK = true\n" + safe_release_code)

# 2. Change initLocked to suspend
code = code.replace("private fun initLocked", "private suspend fun initLocked")

# 3. Replace tts?.release() and tts = null with safeReleaseTts()
# Note: we only want to replace the TTS ones, NOT audioTrack or toneGen
code = code.replace("tts?.release()\n            tts = null", "safeReleaseTts()")
code = code.replace("tts?.release()\n            tts = null", "safeReleaseTts()")
code = code.replace("tts?.release()\n            tts = null", "safeReleaseTts()")

# Catch any space variations
code = re.sub(r'tts\?\.release\(\)\s*tts = null', 'safeReleaseTts()', code)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("TTSEngine patched for native crash.")
