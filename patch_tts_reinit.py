with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

search = """        if (tts == null) {
            if (context != null) initLocked(context, currentLang)
            if (tts == null) return@withContext
        }"""
replace = """        if (context != null) {
            // Always ensure the correct model is loaded for currentLang
            initLocked(context, currentLang)
        }
        if (tts == null) return@withContext"""
code = code.replace(search, replace)
with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
