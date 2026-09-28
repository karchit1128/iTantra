import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''                    } else if (isWalkieTalkieOn) {
                        // "it should work like a walkie talkie"
                        // Normal chatter auto-plays in the background ONLY if Walkie-Talkie mode is active
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, false)
                    }
                    // "if turned off it should work like a phone"
                    // Meaning if !isWalkieTalkieOn and !isAlert, it does NOTHING (just silently receives text).
                }'''

replacement = '''                    } else if (isWalkieTalkieOn) {
                        // "it should work like a walkie talkie"
                        // Normal chatter auto-plays in the background ONLY if Walkie-Talkie mode is active
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, false)
                    } else {
                        // "if turned off it should work like a phone"
                        // Send a standard notification ding/vibration like WhatsApp
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val cleanText = message.removePrefix("[TTS]").replace(Regex("\\\\[ID:[^\\\\]]+\\\\]"), "").replace(Regex("\\\\[PRIO:[^\\\\]]+\\\\]"), "")
                        
                        val notif = NotificationCompat.Builder(this@MeshForegroundService, CHANNEL_ID)
                            .setContentTitle("New Offline Message")
                            .setContentText(cleanText)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .build()
                            
                        notificationManager.notify((1000..9999).random(), notif)
                    }
                }'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found in TTSEngine.kt.')
