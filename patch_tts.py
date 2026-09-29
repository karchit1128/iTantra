import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TTSEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/$onnxFile",
                        tokens = "$modelDir/tokens.txt",
                        dataDir = espeakDir.absolutePath
                    ),'''

replacement = '''                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/$onnxFile",
                        tokens = "$modelDir/tokens.txt", // For Piper, some versions need tokens.txt, others need json. But we can ALSO specify it if it crashes. wait!
                        // Actually, wait, does OfflineTtsVitsModelConfig have another parameter?
                    ),'''
# Wait, let me just change tokens to the json file and see if it compiles or if it works.
replacement = '''                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/$onnxFile",
                        tokens = "$modelDir/$onnxFile.json",
                        dataDir = espeakDir.absolutePath
                    ),'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched TTSEngine successfully!')
else:
    print('Target not found in TTSEngine.kt.')
