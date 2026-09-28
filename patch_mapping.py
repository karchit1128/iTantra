import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''            var modelDir = "vits-piper-en_US-amy-low"
            var onnxFile = "en_US-amy-low.onnx"
            
            if (language == "Hindi") {
                modelDir = "vits-piper-hi_IN-rohan-medium"
                onnxFile = "hi_IN-rohan-medium.onnx"
            } else if (language != "English") {
                // For other languages, we rely entirely on Android TTS
                currentLang = language
                return@withContext
            }'''

replacement = '''            val (modelDir, onnxFile) = when (language) {
                "Hindi"     -> Pair("vits-piper-hi_IN-rohan-medium", "hi_IN-rohan-medium.onnx")
                "Gujarati"  -> Pair("vits-piper-gu_IN-medium", "gu_IN-medium.onnx")
                "Marathi"   -> Pair("vits-piper-mr_IN-medium", "mr_IN-medium.onnx")
                "Tamil"     -> Pair("vits-piper-ta_IN-medium", "ta_IN-medium.onnx")
                "Telugu"    -> Pair("vits-piper-te_IN-medium", "te_IN-medium.onnx")
                "Kannada"   -> Pair("vits-piper-kn_IN-medium", "kn_IN-medium.onnx")
                "Malayalam" -> Pair("vits-piper-ml_IN-medium", "ml_IN-medium.onnx")
                "Bengali"   -> Pair("vits-piper-bn_IN-medium", "bn_IN-medium.onnx")
                "Odia"      -> Pair("vits-piper-or_IN-medium", "or_IN-medium.onnx")
                else        -> Pair("vits-piper-en_US-amy-low", "en_US-amy-low.onnx") // English default
            }'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found in TTSEngine.kt.')
