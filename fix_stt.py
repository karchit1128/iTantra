
import re

with open("app/src/main/java/com/example/itantra/ml/STTEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()

# BUG-8 Fix: Add crash recovery before the isInitialized check
old = '    suspend fun transcribe(audioData: ShortArray, targetLanguage: String = "English"): SttResult = withContext(Dispatchers.Default) {\n        if (!isInitialized || recognizer == null) return@withContext SttResult.Empty()'
new = '''    suspend fun transcribe(audioData: ShortArray, targetLanguage: String = "English"): SttResult = withContext(Dispatchers.Default) {
        // BUG-8 Fix: If STT crashed (isInitialized=true but recognizer=null), reset so next call re-inits
        if (isInitialized && recognizer == null) {
            android.util.Log.w(TAG, "STT crash recovery: resetting isInitialized flag")
            isInitialized = false
        }
        if (!isInitialized || recognizer == null) return@withContext SttResult.Empty()'''

if old not in code:
    print("BUG-8 marker NOT FOUND")
else:
    code = code.replace(old, new)
    print("BUG-8 applied")

# BUG-3 Fix: Replace the strict gate with lenient 80% mismatch gate
old3 = '''            // STRICT LANGUAGE ISOLATION (Language-ID Gate) - Now keeps digits and basic punctuation
            val allowedRegex = when (targetLanguage) {
                "Hindi", "Marathi" -> Regex("[^\\\\u0900-\\\\u097F\\\\s0-9.,!?']")
                "Gujarati" -> Regex("[^\\\\u0A80-\\\\u0AFF\\\\s0-9.,!?']")
                "Bengali" -> Regex("[^\\\\u0980-\\\\u09FF\\\\s0-9.,!?']")
                "Tamil" -> Regex("[^\\\\u0B80-\\\\u0BFF\\\\s0-9.,!?']")
                "Telugu" -> Regex("[^\\\\u0C00-\\\\u0C7F\\\\s0-9.,!?']")
                "Kannada" -> Regex("[^\\\\u0C80-\\\\u0CFF\\\\s0-9.,!?']")
                "Malayalam" -> Regex("[^\\\\u0D00-\\\\u0D7F\\\\s0-9.,!?']")
                "Odia" -> Regex("[^\\\\u0B00-\\\\u0B7F\\\\s0-9.,!?']")
                "English" -> Regex("[^a-zA-Z0-9\\\\s.,!?']")
                else -> Regex("[^a-zA-Z0-9\\\\s.,!?']")
            }
            val filteredText = rawText.replace(allowedRegex, "").replace(Regex("\\\\s+"), " ").trim()
            
            Log.d("STT_GATE", "Target: $targetLanguage | RAW: \'$rawText\' | FILTERED: \'$filteredText\'")
            
            if (filteredText.isEmpty() && rawText.isNotBlank()) {
                return@withContext SttResult.LangMismatch(rawText, "Language mismatch: resend or send as-is")
            }
            return@withContext SttResult.Success(filteredText, rawText)'''

new3 = '''            // BUG-3 Fix: LENIENT LANGUAGE GATE
            // Dolphin outputs romanized/mixed text. Only reject if >80% of alpha chars are wrong-script.
            val scriptRegex = when (targetLanguage) {
                "Hindi", "Marathi" -> Regex("[\\u0900-\\u097F]")
                "Gujarati" -> Regex("[\\u0A80-\\u0AFF]")
                "Bengali" -> Regex("[\\u0980-\\u09FF]")
                "Tamil" -> Regex("[\\u0B80-\\u0BFF]")
                "Telugu" -> Regex("[\\u0C00-\\u0C7F]")
                "Kannada" -> Regex("[\\u0C80-\\u0CFF]")
                "Malayalam" -> Regex("[\\u0D00-\\u0D7F]")
                "Odia" -> Regex("[\\u0B00-\\u0B7F]")
                "English" -> Regex("[a-zA-Z]")
                else -> Regex("[a-zA-Z]")
            }
            val allAlpha = rawText.filter { it.isLetter() }
            val targetScriptChars = scriptRegex.findAll(allAlpha).count()
            val mismatchRatio = if (allAlpha.isNotEmpty()) 1.0 - (targetScriptChars.toDouble() / allAlpha.length) else 0.0
            Log.d("STT_GATE", "Target: $targetLanguage | RAW: \'$rawText\' | Mismatch: $mismatchRatio")
            if (mismatchRatio > 0.8 && allAlpha.isNotEmpty()) {
                return@withContext SttResult.LangMismatch(rawText, "Language mismatch: resend or send as-is")
            }
            return@withContext SttResult.Success(rawText, rawText)'''

if old3 not in code:
    print("BUG-3 marker NOT FOUND — trying regex approach")
    # find from STRICT LANGUAGE ISOLATION to the end of the block
    match = re.search(r"// STRICT LANGUAGE ISOLATION.*?return@withContext SttResult\.Success\(filteredText, rawText\)", code, re.DOTALL)
    if match:
        code = code[:match.start()] + new3.strip() + code[match.end():]
        print("BUG-3 applied via regex")
    else:
        print("BUG-3 regex also failed")
else:
    code = code.replace(old3, new3)
    print("BUG-3 applied")

with open("app/src/main/java/com/example/itantra/ml/STTEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)

print("STTEngine.kt written")
