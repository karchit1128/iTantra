with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("\"[ID:$msgId][PRIO:$prio][TTS]", "\"[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]")
with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
