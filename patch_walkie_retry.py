with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("meshManager.broadcastMessage(\"[LANG:$selectedLanguage][ID:$msgId]", "meshManager.sendWithRetry(msgId, \"[LANG:$selectedLanguage][ID:$msgId]")

with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
