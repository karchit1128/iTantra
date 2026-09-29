import re

filepath = 'app/src/main/java/com/example/itantra/MainActivity.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# Add java.io.File import if missing
if 'import java.io.File' not in code:
    code = code.replace('import android.os.Bundle', 'import android.os.Bundle\nimport java.io.File')

old_handler = r'''        Thread\.setDefaultUncaughtExceptionHandler \{ thread, throwable ->
            val log = android\.util\.Log\.getStackTraceString\(throwable\)'''

new_handler = '''        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val log = android.util.Log.getStackTraceString(throwable)
            try {
                val f = File(getExternalFilesDir(null), "last_crash.txt")
                f.writeText("Thread: \\n" + log)
            } catch (e: Exception) {
                // Ignore file write errors during crash
            }'''

code = re.sub(old_handler, new_handler, code)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)
print("Crash handler patched.")
