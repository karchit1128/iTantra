lines = []
with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "rb") as f:
    lines = f.readlines()

out = []
in_when = False
for line in lines:
    try:
        decoded = line.decode('utf-8', errors='ignore')
    except:
        decoded = line.decode('cp1252', errors='ignore')
        
    if "val testText = when(lang) {" in decoded:
        in_when = True
        out.append(b'                                val testText = when(lang) {\n')
        out.append(b'                                    "Hindi" -> "\xe0\xa4\xa8\xe0\xa4\xae\xe0\xa4\xb8\xe0\xa5\x8d\xe0\xa4\xa4\xe0\xa5\x87"\n')
        out.append(b'                                    "Marathi" -> "\xe0\xa4\xa8\xe0\xa4\xae\xe0\xa4\xb8\xe0\xa5\x8d\xe0\xa4\x95\xe0\xa4\xbe\xe0\xa4\xb0"\n')
        out.append(b'                                    "Gujarati" -> "\xe0\xaa\xa8\xe0\xaa\xae\xe0\xaa\xb8\xe0\xab\x8d\xe0\xaa\xa4\xe0\xab\x87"\n')
        out.append(b'                                    "Tamil" -> "\xe0\xae\xb5\xe0\xae\xa3\xe0\xae\x95\xe0\xaf\x8d\xe0\xae\x95\xe0\xae\xae\xe0\xaf\x8d"\n')
        out.append(b'                                    "Telugu" -> "\xe0\xb0\xa8\xe0\xb0\xae\xe0\xb0\xb8\xe0\xb1\x8d\xe0\xb0\x95\xe0\xb0\xbe\xe0\xb0\xb0\xe0\xb0\x82"\n')
        out.append(b'                                    "Kannada" -> "\xe0\xb2\xa8\xe0\xb2\xae\xe0\xb2\xb8\xe0\xb3\x8d\xe0\xb2\x95\xe0\xb2\xbe\xe0\xb2\xb0"\n')
        out.append(b'                                    "Malayalam" -> "\xe0\xb4\xa8\xe0\xb4\xae\xe0\xb4\xb8\xe0\xb5\x8d\xe0\xb4\x95\xe0\xb4\xbe\xe0\xb4\xb0\xe0\xb4\x82"\n')
        out.append(b'                                    "Bengali" -> "\xe0\xa6\xa8\xe0\xa6\xae\xe0\xa6\xb8\xe0\xa7\x8d\xe0\xa6\x95\xe0\xa6\xbe\xe0\xa6\xb0"\n')
        out.append(b'                                    "Odia" -> "\xe0\xac\xa8\xe0\xac\xae\xe0\xac\xb8\xe0\xac\x8d\xe0\xac\x95\xe0\xac\xbe\xe0\xac\xb0"\n')
        out.append(b'                                    else -> "Hello, this is a test."\n')
        out.append(b'                                }\n')
        continue
        
    if in_when:
        if "}" in decoded and "else" not in decoded and "->" not in decoded:
            in_when = False
        continue
        
    out.append(line)

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "wb") as f:
    for line in out:
        f.write(line)
