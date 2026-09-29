import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target1 = '''import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext'''

replacement1 = '''import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock'''

target2 = '''    var currentLang: String = ""
        private set

    suspend fun init(context: Context, language: String = "English") = withContext(Dispatchers.IO) {
        if (tts != null && currentLang == language) return@withContext
        
        Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")
        
        try {
            tts?.release()
            tts = null'''

replacement2 = '''    var currentLang: String = ""
        private set
        
    private val mutex = Mutex()

    suspend fun init(context: Context, language: String = "English") = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (tts != null && currentLang == language) return@withLock
            
            Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")
            
            try {
                tts?.release()
                tts = null'''

target3 = '''            Log.d(TAG, "Sherpa-ONNX TTS initialized.")
        } catch (e: Throwable) {
            Log.e(TAG, "Sherpa TTS Init Failed for $language. Model files may be missing.", e)
            tts = null // Ensure it's null on failure
        }
    }'''

replacement3 = '''            Log.d(TAG, "Sherpa-ONNX TTS initialized.")
            } catch (e: Throwable) {
                Log.e(TAG, "Sherpa TTS Init Failed for $language. Model files may be missing.", e)
                tts = null // Ensure it's null on failure
            }
        }
    }'''

if target1 in text and target2 in text and target3 in text:
    text = text.replace(target1, replacement1)
    text = text.replace(target2, replacement2)
    text = text.replace(target3, replacement3)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched mutex successfully!')
else:
    print('Targets not found in TTSEngine.kt.')
