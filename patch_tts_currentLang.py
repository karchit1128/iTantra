with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("    var currentLang: String = \"\"\n        private set", "    var currentLang: String = \"\"")
with open("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)
