with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "r", encoding="utf-8") as f:
    code = f.read()

search = """                    if (message.startsWith("[TTS]")) {
                        val prefs = getSharedPreferences("itnt_settings", Context.MODE_PRIVATE)
                        val isWalkieTalkieOn = prefs.getBoolean("mesh_visible", false)
                        val isAlert = message.contains("RED") || message.contains("Priority RED") || message.contains("ALERT")
                        
                        if (isAlert) {
                            // "alert type messages will be announced at highest volume non-interruptible"
                            // ALWAYS play Red alerts at max volume, even if Walkie Talkie mode is off!
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, true)
                        } else if (isWalkieTalkieOn && com.example.itantra.MainActivity.isAppInForeground) {
                            // "TTS should work only if the app is running on the screen and in conversations."
                            // Normal chatter auto-plays ONLY if Walkie-Talkie mode is active AND App is in foreground
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, false)
                        } else {
                            // "if the mssge is in the green or yellow then the notification will be sent to the user if the app is closed or minimised"
                            // Send a standard push notification
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val cleanText = message.removePrefix("[TTS]").replace(Regex("\\[ID:[^\\]]+\\]"), "").replace(Regex("\\[PRIO:[^\\]]+\\]"), "")"""
replace = """                    if (message.startsWith("[TTS]")) {
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
                        val isAlert = cleanMessage.contains("Priority RED Alert")
                        
                        if (isAlert) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                        } else if (isWalkieTalkieOn && com.example.itantra.MainActivity.isAppInForeground) {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                        } else {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val cleanText = cleanMessage"""
code = code.replace(search, replace)
with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "w", encoding="utf-8") as f:
    f.write(code)
