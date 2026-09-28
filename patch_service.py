import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/mesh/MeshForegroundService.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''                    if (isAlert) {
                        // "alert type messages will be announced at highest volume non-interruptible"
                        // ALWAYS play Red alerts at max volume, even if Walkie Talkie mode is off!
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, true)
                    } else if (isWalkieTalkieOn) {
                        // "it should work like a walkie talkie"
                        // Normal chatter auto-plays in the background ONLY if Walkie-Talkie mode is active
                        com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), this@MeshForegroundService, false)
                    } else {
                        // "if turned off it should work like a phone"
                        // Send a standard notification ding/vibration like WhatsApp
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager'''

replacement = '''                    if (isAlert) {
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
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched MeshForegroundService successfully!')
else:
    print('Target not found in MeshForegroundService.kt.')
