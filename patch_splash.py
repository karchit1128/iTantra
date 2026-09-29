import re

filepath = 'app/src/main/java/com/example/itantra/ui/screens/SplashScreen.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

old_effect = r'''    LaunchedEffect\(Unit\) \{
        val prefs = context\.getSharedPreferences\("itnt_settings", android\.content\.Context\.MODE_PRIVATE\)
        val targetLang = prefs\.getString\("target_language", "English"\) \?: "English"
        
        loadingText = "Loading STT Model\.\.\."
        com\.example\.itantra\.ml\.STTEngine\.init\(context\)
        
        loadingText = "Loading TTS \(\\)\.\.\."
        com\.example\.itantra\.ml\.TTSEngine\.init\(context, targetLang\)
        
        loadingText = "Starting Wi-Fi Aware Daemon\.\.\."
        onTimeout\(\)
    \}'''

new_effect = '''    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("itnt_settings", android.content.Context.MODE_PRIVATE)
        val targetLang = prefs.getString("target_language", "English") ?: "English"
        
        try {
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                loadingText = "Loading STT Model..."
                try {
                    com.example.itantra.ml.STTEngine.init(context)
                } catch(e: Exception) {
                    android.util.Log.e("SplashScreen", "STT Init failed", e)
                    android.widget.Toast.makeText(context, "STT preload failed", android.widget.Toast.LENGTH_SHORT).show()
                }
                
                loadingText = "Loading TTS ()..."
                try {
                    com.example.itantra.ml.TTSEngine.init(context, targetLang)
                } catch(e: Exception) {
                    android.util.Log.e("SplashScreen", "TTS Init failed", e)
                    android.widget.Toast.makeText(context, "TTS preload failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch(e: Exception) {
            android.util.Log.e("SplashScreen", "Startup init crashed", e)
        }
        
        loadingText = "Starting UI..."
        onTimeout()
    }'''

code = re.sub(old_effect, new_effect, code)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("SplashScreen patched.")
