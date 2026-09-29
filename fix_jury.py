import re

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "r", encoding="utf-8", errors='ignore') as f:
    code = f.read()

# Let's replace the whole try-catch block for the language loop
search_pattern = r"val testText = when\(lang\) \{.*?\n\s+else -> \"Hello, this is a test.\"\n\s+\}"
replace_str = """val testText = when(lang) {
                                "Hindi" -> "नमस्ते"
                                "Marathi" -> "नमस्कार"
                                "Gujarati" -> "નમસ્તે"
                                "Tamil" -> "வணக்கம்"
                                "Telugu" -> "నమస్కారం"
                                "Kannada" -> "ನಮಸ್ಕಾರ"
                                "Malayalam" -> "നമസ്കാരം"
                                "Bengali" -> "নমস্কার"
                                "Odia" -> "ନମସ୍କାର"
                                else -> "Hello, this is a test."
                            }"""

new_code = re.sub(search_pattern, replace_str, code, flags=re.DOTALL)

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "w", encoding="utf-8") as f:
    f.write(new_code)
