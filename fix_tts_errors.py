import re

filepath = 'app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Fix initLocked
init_locked_match = re.search(r'private suspend fun initLocked\([\s\S]*?\{[\s\S]*?if\s*\(tts != null[\s\S]*?return\s*\}', code)
if init_locked_match:
    new_init_locked = '''    private suspend fun initLocked(context: Context, language: String = "English") {
        if (ttsCache.containsKey(language)) {
            currentLang = language
            loadedLang = language
            return
        }'''
    code = code[:init_locked_match.start()] + new_init_locked + code[init_locked_match.end():]

# 2. Fix benchmarkTTS
benchmark_old = r'''        if \(!ttsCache\.containsKey\(currentLang\) && context != null\) \{[\s\S]*?if \(currentTts == null\) return@withContext Pair\(0L, 0f\)[\s\S]*?\}'''
benchmark_new = '''        if (!ttsCache.containsKey(currentLang) && context != null) {
            initLocked(context, if (currentLang.isEmpty()) "English" else currentLang)
        }
        val currentTts = ttsCache[currentLang]
        if (currentTts == null) return@withContext Pair(0L, 0f)
'''
code = re.sub(benchmark_old, benchmark_new, code)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("Errors fixed.")
