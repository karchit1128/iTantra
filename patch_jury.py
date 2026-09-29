with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()

import re
# Replace the garbage strings with proper unicode strings
code = re.sub(r'"Hindi"\s*->\s*".*?"', '"Hindi" -> "नमस्ते"', code)
code = re.sub(r'"Marathi"\s*->\s*".*?"', '"Marathi" -> "नमस्कार"', code)
code = re.sub(r'"Telugu"\s*->\s*".*?"', '"Telugu" -> "నమస్కారం"', code)
code = re.sub(r'"Malayalam"\s*->\s*".*?"', '"Malayalam" -> "നമസ്കാരം"', code)
code = re.sub(r'"Tamil"\s*->\s*".*?"', '"Tamil" -> "வணக்கம்"', code)
code = re.sub(r'"Gujarati"\s*->\s*".*?"', '"Gujarati" -> "નમસ્તે"', code)
code = re.sub(r'"Bengali"\s*->\s*".*?"', '"Bengali" -> "নমস্কার"', code)
code = re.sub(r'"Odia"\s*->\s*".*?"', '"Odia" -> "ନମସ୍କାର"', code)
code = re.sub(r'"Kannada"\s*->\s*".*?"', '"Kannada" -> "ನಮಸ್ಕಾರ"', code)

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
