import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(Dispatchers.IO) {
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

        if (tts == null) {'''

replacement = '''    suspend fun synthesizeAndPlay(text: String, context: Context? = null, isAlert: Boolean = false) = withContext(Dispatchers.IO) {
        if (tts == null) {'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found in TTSEngine.kt.')
