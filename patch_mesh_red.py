with open("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", "r", encoding="utf-8") as f:
    code = f.read()

search = """                var displayMsg = inner
                if (inner.startsWith("[TTS]")) {
                    displayMsg = "[TTS][$msgLang]" + inner.removePrefix("[TTS]")
                }"""
replace = """                var displayMsg = inner
                if (inner.startsWith("[TTS]")) {
                    val pTag = if (msgPriority == "RED") "[RED]" else ""
                    displayMsg = "[TTS][$msgLang]$pTag" + inner.removePrefix("[TTS]")
                }"""
code = code.replace(search, replace)
with open("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", "w", encoding="utf-8") as f:
    f.write(code)
