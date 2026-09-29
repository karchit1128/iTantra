with open("app/src/main/java/com/example/itantra/ml/STTEngine.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("SttResult.Empty\n", "SttResult.Empty()\n")
with open("app/src/main/java/com/example/itantra/ml/STTEngine.kt", "w", encoding="utf-8") as f:
    f.write(code)

with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("else com.example.itantra.ml.SttResult.Empty", "else com.example.itantra.ml.SttResult.Empty()")
with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
