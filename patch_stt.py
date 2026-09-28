import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/STTEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''    suspend fun transcribe(audioData: ShortArray): String = withContext(Dispatchers.Default) {
        if (!isInitialized || recognizer == null) return@withContext ""
        
        Log.d(TAG, "Sherpa STT decoding ${audioData.size} samples...")
        try {
            val stream = recognizer!!.createStream()
            val floatArray = FloatArray(audioData.size) { i -> audioData[i] / 32768.0f }
            stream.acceptWaveform(floatArray, 16000)
            recognizer!!.decode(stream)
            val result = recognizer!!.getResult(stream)
            val rawText = result.text
            stream.release()
            
            // STRICT REQUIREMENT: Only 10 languages (English + 9 Indian languages)
            // English: U+0000-U+007F
            // Devanagari (Hindi/Marathi): U+0900-U+097F
            // Bengali: U+0980-U+09FF
            // Gujarati: U+0A80-U+0AFF
            // Odia: U+0B00-U+0B7F
            // Tamil: U+0B80-U+0BFF
            // Telugu: U+0C00-U+0C7F
            // Kannada: U+0C80-U+0CFF
            // Malayalam: U+0D00-U+0D7F
            val allowedRegex = Regex("[^\\u0000-\\u007F\\u0900-\\u097F\\u0980-\\u09FF\\u0A80-\\u0AFF\\u0B00-\\u0B7F\\u0B80-\\u0BFF\\u0C00-\\u0C7F\\u0C80-\\u0CFF\\u0D00-\\u0D7F]")
            val filteredText = rawText.replace(allowedRegex, "").replace(Regex("\\\\s+"), " ").trim()
            
            return@withContext filteredText'''

replacement = '''    suspend fun transcribe(audioData: ShortArray, targetLanguage: String = "English"): String = withContext(Dispatchers.Default) {
        if (!isInitialized || recognizer == null) return@withContext ""
        
        Log.d(TAG, "Sherpa STT decoding ${audioData.size} samples for $targetLanguage...")
        try {
            val stream = recognizer!!.createStream()
            val floatArray = FloatArray(audioData.size) { i -> audioData[i] / 32768.0f }
            stream.acceptWaveform(floatArray, 16000)
            recognizer!!.decode(stream)
            val result = recognizer!!.getResult(stream)
            val rawText = result.text
            stream.release()
            
            // STRICT LANGUAGE ISOLATION
            val allowedRegex = when (targetLanguage) {
                "Hindi", "Marathi" -> Regex("[^\\\\u0900-\\\\u097F\\\\s]")
                "Gujarati" -> Regex("[^\\\\u0A80-\\\\u0AFF\\\\s]")
                "Bengali" -> Regex("[^\\\\u0980-\\\\u09FF\\\\s]")
                "Tamil" -> Regex("[^\\\\u0B80-\\\\u0BFF\\\\s]")
                "Telugu" -> Regex("[^\\\\u0C00-\\\\u0C7F\\\\s]")
                "Kannada" -> Regex("[^\\\\u0C80-\\\\u0CFF\\\\s]")
                "Malayalam" -> Regex("[^\\\\u0D00-\\\\u0D7F\\\\s]")
                "Odia" -> Regex("[^\\\\u0B00-\\\\u0B7F\\\\s]")
                "English" -> Regex("[^a-zA-Z0-9\\\\s.,!?']")
                else -> Regex("[^a-zA-Z0-9\\\\s.,!?']")
            }
            val filteredText = rawText.replace(allowedRegex, "").replace(Regex("\\\\s+"), " ").trim()
            
            return@withContext filteredText'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched STTEngine successfully!')
else:
    print('Target not found in STTEngine.kt.')
