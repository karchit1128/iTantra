import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/STTEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''            val stream = recognizer!!.createStream()
            val GAIN = 3.0f
            val floatArray = FloatArray(audioData.size) { i -> 
                val floatVal = (audioData[i] / 32768.0f) * GAIN
                floatVal.coerceIn(-1.0f, 1.0f) 
            }'''

replacement = '''            val stream = recognizer!!.createStream()
            val GAIN = 1.0f // Reverted back to 1.0f to prevent heavy audio clipping/distortion
            val floatArray = FloatArray(audioData.size) { i -> 
                val floatVal = (audioData[i] / 32768.0f) * GAIN
                floatVal.coerceIn(-1.0f, 1.0f) 
            }'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched GAIN successfully!')
else:
    print('Target not found in STTEngine.kt.')
