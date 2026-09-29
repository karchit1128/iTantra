import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target1 = '''import kotlinx.coroutines.withContext
import com.k2fsa.sherpa.onnx.*'''

replacement1 = '''import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import com.k2fsa.sherpa.onnx.*'''

target2 = '''    private var currentLang: String = ""
    
    // Fallback Android TTS'''

replacement2 = '''    var currentLang: String = ""
        private set
        
    private val mutex = Mutex()
    
    // Fallback Android TTS'''

target3 = '''    suspend fun init(context: Context, language: String = "English") = withContext(Dispatchers.IO) {
        if (tts != null && currentLang == language) return@withContext
        
        Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")'''

replacement3 = '''    suspend fun init(context: Context, language: String = "English") = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (tts != null && currentLang == language) return@withLock
            
            Log.d(TAG, "Sherpa-ONNX TTS initializing for $language...")'''

target4 = '''        } catch (e: Throwable) {
            Log.e(TAG, "Sherpa TTS Init Failed", e)
        }
    }'''

replacement4 = '''        } catch (e: Throwable) {
            Log.e(TAG, "Sherpa TTS Init Failed", e)
        }
        }
    }'''

target5 = '''    suspend fun synthesizeAndPlay(text: String, context: Context? = null) = withContext(Dispatchers.IO) {'''

replacement5 = '''    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(Dispatchers.IO) {'''

target6 = '''            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                audio.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                shortArray.size * 2,
                AudioTrack.MODE_STATIC
            )'''

replacement6 = '''            val usage = if (isAlert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
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
            }'''

text = text.replace(target1, replacement1)
text = text.replace(target2, replacement2)
text = text.replace(target3, replacement3)
text = text.replace(target4, replacement4)
text = text.replace(target5, replacement5)
text = text.replace(target6, replacement6)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)
print("Restored TTSEngine safely!")
