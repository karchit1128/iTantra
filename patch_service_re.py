with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "r", encoding="utf-8") as f:
    code = f.read()

import re

# We will use regex to find the TTS block
pattern = re.compile(r'if \(message\.startsWith\("\[TTS\]"\)\) \{.*?\n\s+val cleanText = message\.removePrefix\("\[TTS\]"\)\.replace\(Regex\("\\\[ID:\[\^\\\]\]\+\\\]"\), ""\)\.replace\(Regex\("\\\[PRIO:\[\^\\\]\]\+\\\]"\), ""\)', re.DOTALL)
match = pattern.search(code)
if match:
    replace = """if (message.startsWith("[TTS]")) {
                        var msgLang = "English"
                        var cleanMessage = message.removePrefix("[TTS]")
                        if (cleanMessage.startsWith("[")) {
                            val end = cleanMessage.indexOf("]")
                            if (end != -1) {
                                msgLang = cleanMessage.substring(1, end)
                                cleanMessage = cleanMessage.substring(end + 1)
                            }
                        }
                        
                        // Set current language to sender's language so TTS Engine picks the right dict
                        com.example.itantra.ml.TTSEngine.currentLang = msgLang

                        val prefs = getSharedPreferences("itnt_settings", Context.MODE_PRIVATE)
                        val isWalkieTalkieOn = prefs.getBoolean("mesh_visible", false)
                        val isAlert = cleanMessage.contains("Priority RED") || cleanMessage.contains("Priority RED Alert")
                        
                        if (isAlert) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                        } else if (isWalkieTalkieOn && com.example.itantra.MainActivity.isAppInForeground) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                        } else {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val cleanText = cleanMessage.replace(Regex("\\[ID:[^\\]]+\\]"), "").replace(Regex("\\[PRIO:[^\\]]+\\]"), "")"""
    code = code.replace(match.group(0), replace)
    with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "w", encoding="utf-8") as f:
        f.write(code)
    print("PATCH APPLIED")
else:
    print("MATCH NOT FOUND")
