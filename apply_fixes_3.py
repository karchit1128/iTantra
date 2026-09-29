import re

# 1. Fix Arabic/Urdu translation bug by filtering out non-supported scripts
stt_path = "app/src/main/java/com/example/itantra/ml/STTEngine.kt"
with open(stt_path, "r", encoding="utf-8") as f:
    stt_content = f.read()

old_stt = """            val rawText = recognizer?.result?.text ?: ""
            
            // GATE: Check language mismatch
            if (rawText.isNotEmpty() && targetLanguage != "English") {"""

new_stt = """            var rawText = recognizer?.result?.text ?: ""
            // BUG-23 Fix: Filter out Arabic/Urdu scripts from the multilingual STT to lock it to 10 languages
            rawText = rawText.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
            if (rawText.isEmpty()) return@withContext SttResult.Empty()
            
            // GATE: Check language mismatch
            if (rawText.isNotEmpty() && targetLanguage != "English") {"""

stt_content = stt_content.replace(old_stt, new_stt)
with open(stt_path, "w", encoding="utf-8") as f:
    f.write(stt_content)


# 2. Fix the "Kid Voice" for RED alerts
tts_path = "app/src/main/java/com/example/itantra/ml/TTSEngine.kt"
with open(tts_path, "r", encoding="utf-8") as f:
    tts_content = f.read()

old_tts = """                // Increase Pitch/Speed for RED Alert
                try {
                    val defaultRate = audioTrack.playbackRate
                    audioTrack.playbackRate = (defaultRate * 1.3f).toInt()
                } catch (e: Exception) {
                    Log.e(TAG, "Playback rate change failed", e)
                }"""

new_tts = """                // Removed 1.3x pitch increase so it doesn't sound like a kid
                // Just use max volume + siren."""

tts_content = tts_content.replace(old_tts, new_tts)

with open(tts_path, "w", encoding="utf-8") as f:
    f.write(tts_content)


# 3. Add active Bluetooth connector
bt_path = "app/src/main/java/com/example/itantra/mesh/BluetoothFallbackManager.kt"
with open(bt_path, "r", encoding="utf-8") as f:
    bt_content = f.read()

if "fun connectToPairedDevices()" not in bt_content:
    old_bt = """    fun startListening() {"""
    new_bt = """    @SuppressLint("MissingPermission")
    fun connectToPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        scope.launch {
            val pairedDevices = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                try {
                    val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                    socket.connect()
                    Log.d(TAG, "Bluetooth Fallback successfully connected OUT to: ${device.name}")
                    activeSockets.add(socket)
                    // Start listening to this out-socket
                    launch {
                        val dataIn = java.io.DataInputStream(socket.inputStream)
                        while (true) {
                            val bytes = dataIn.readInt()
                            if (bytes > 0) {
                                val buffer = ByteArray(bytes)
                                dataIn.readFully(buffer)
                                val encryptedMessage = String(buffer)
                                val decrypted = CryptoEngine.decrypt(encryptedMessage)
                                if (decrypted.isNotEmpty()) onMessageReceived(decrypted)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Normal if the other device isn't running the server right now
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun startListening() {"""
    bt_content = bt_content.replace(old_bt, new_bt)

with open(bt_path, "w", encoding="utf-8") as f:
    f.write(bt_content)


# 4. Trigger BT connect in MeshNetworkManager when Wifi disconnects
mesh_path = "app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt"
with open(mesh_path, "r", encoding="utf-8") as f:
    mesh_content = f.read()

old_mesh = """            wifiDirectHelper?.onConnectionChanged = { count ->
                _connectedPeersCount.value = count
            }"""

new_mesh = """            wifiDirectHelper?.onConnectionChanged = { count ->
                _connectedPeersCount.value = count
                if (count == 0) {
                    bluetoothFallback?.connectToPairedDevices()
                }
            }"""

if "connectToPairedDevices()" not in mesh_content:
    mesh_content = mesh_content.replace(old_mesh, new_mesh)
    with open(mesh_path, "w", encoding="utf-8") as f:
        f.write(mesh_content)

print("Applied quick fixes to STT, TTS, BT, Mesh")
