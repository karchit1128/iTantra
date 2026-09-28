import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '                actions = {\n                    Surface(shape = RoundedCornerShape(20.dp), color = NdrfOrange.copy(alpha = 0.12f), modifier = Modifier.padding(end = 6.dp)) {\n                        Text(selectedLanguage, color = NdrfOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))\n                    }'

replacement = '                actions = {\n                    var expandedLang by remember { mutableStateOf(false) }\n                    val languages = listOf("English", "Hindi", "Bengali", "Telugu", "Marathi", "Tamil", "Gujarati", "Kannada", "Odia", "Malayalam")\n                    Box {\n                        Surface(shape = RoundedCornerShape(20.dp), color = NdrfOrange.copy(alpha = 0.12f), modifier = Modifier.padding(end = 6.dp).clickable { expandedLang = true }) {\n                            Text(selectedLanguage, color = NdrfOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))\n                        }\n                        DropdownMenu(expanded = expandedLang, onDismissRequest = { expandedLang = false }) {\n                            languages.forEach { lang ->\n                                DropdownMenuItem(text = { Text(lang) }, onClick = { prefs.edit().putString("target_language", lang).apply(); expandedLang = false })\n                            }\n                        }\n                    }'

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found.')
