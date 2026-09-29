with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "r", encoding="utf-8") as f:
    code = f.read()

search = """                    // FIX F4: only alert if it is Priority RED
                    val isAlert = cleanMessage.contains("Priority RED Alert")
                    
                    if (isAlert) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                    } else if (isWalkieTalkieOn && com.example.itantra.MainActivity.isAppInForeground) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                    } else {
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val cleanText = cleanMessage.replace(Regex("\\[ID:[^\\]]+\\]"), "").replace(Regex("\\[PRIO:[^\\]]+\\]"), "")"""
replace = """                    // Check for [RED] tag added by MeshNetworkManager
                    val isAlert = cleanMessage.startsWith("[RED]")
                    if (isAlert) {
                        cleanMessage = cleanMessage.removePrefix("[RED]")
                    }
                    
                    if (isAlert) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                    } else if (isWalkieTalkieOn && com.example.itantra.MainActivity.isAppInForeground) {
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                    } else {
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val cleanText = cleanMessage.replace(Regex("\\[ID:[^\\]]+\\]"), "").replace(Regex("\\[PRIO:[^\\]]+\\]"), "")"""
code = code.replace(search, replace)
with open("app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt", "w", encoding="utf-8") as f:
    f.write(code)
