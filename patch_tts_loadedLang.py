with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

# Add loadedLang variable
code = code.replace("    var currentLang: String = \"\"", "    var currentLang: String = \"\"\n    var loadedLang: String = \"\"")

# Fix initLocked logic
search = """    private fun initLocked(context: Context, language: String = "English") {
        if (tts != null && currentLang == language) return"""
replace = """    private fun initLocked(context: Context, language: String = "English") {
        if (tts != null && loadedLang == language) {
            currentLang = language
            return
        }"""
code = code.replace(search, replace)

# Fix where it sets currentLang
search = """            tts = OfflineTts(assetManager = context.assets, config = config)
            currentLang = language
            Log.d(TAG, "Sherpa-ONNX TTS initialized for $language.")"""
replace = """            tts = OfflineTts(assetManager = context.assets, config = config)
            currentLang = language
            loadedLang = language
            Log.d(TAG, "Sherpa-ONNX TTS initialized for $language.")"""
code = code.replace(search, replace)

with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
