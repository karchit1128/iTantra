import re

filepath = 'app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

old_listener = r'''                kotlinx\.coroutines\.GlobalScope\.launch\(kotlinx\.coroutines\.Dispatchers\.IO\) \{
                    com\.example\.itantra\.ml\.TTSEngine\.init\(context, newLang\)
                \}'''
new_listener = '''                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.itantra.ml.TTSEngine.init(context, newLang)
                    } catch (e: Exception) {
                        android.util.Log.e("WalkieTalkieScreen", "Language switch init failed", e)
                    }
                }'''
code = re.sub(old_listener, new_listener, code)

old_send = r'''                                kotlinx\.coroutines\.GlobalScope\.launch\(kotlinx\.coroutines\.Dispatchers\.IO\) \{
                                    try \{'''
new_send = '''                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {'''
code = re.sub(old_send, new_send, code)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("GlobalScope patched in WalkieTalkieScreen.")
