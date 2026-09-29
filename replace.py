import re

with open('app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt', 'r', encoding='utf-8') as f:
    code = f.read()

# Remove WifiAware imports
code = re.sub(r'import android\.net\.wifi\.aware\.\*\n', '', code)

# Remove WifiAware variables
code = re.sub(r'private val wifiAwareManager.*?as\? WifiAwareManager\n\n.*?private var subscribeSession: SubscribeDiscoverySession\? = null\n', 
              'private var wifiDirectHelper: WifiDirectHelper? = null\n', code, flags=re.DOTALL)

# Replace startMesh() body
start_mesh_pattern = r'fun startMesh\(\) \{.*?(?=^\s+private fun startPublishing\(\))'
new_start_mesh = '''fun startMesh() {
        if (bluetoothFallback == null) {
            bluetoothFallback = BluetoothFallbackManager(context) { msg ->
                _incomingMessages.tryEmit(msg)
            }
            bluetoothFallback?.startListening()
        }
        
        if (wifiDirectHelper == null) {
            wifiDirectHelper = WifiDirectHelper(context) { bytes ->
                processReceivedBytes(bytes)
            }
            wifiDirectHelper?.onPeersChanged = { count ->
                _discoveredPeersState.value = (0 until count).map { "Peer-$it" }
            }
            wifiDirectHelper?.start()
        }
        
        startAdaptiveHeartbeat()
    }
'''
code = re.sub(start_mesh_pattern, new_start_mesh, code, flags=re.DOTALL | re.MULTILINE)

# Remove startPublishing and startSubscribing
code = re.sub(r'^\s+@SuppressLint\("MissingPermission"\)\n\s+private fun startPublishing\(\) \{.*?(?=^\s+private fun processReceivedBytes)', '', code, flags=re.DOTALL | re.MULTILINE)

# Remove PeerHandle from processReceivedBytes and handleEncryptedMessage
code = re.sub(r'private fun processReceivedBytes\(peerHandle: PeerHandle, message: ByteArray\)', 'private fun processReceivedBytes(message: ByteArray)', code)
code = re.sub(r'handleEncryptedMessage\(peerHandle, fullMessage\)', 'handleEncryptedMessage(fullMessage)', code)
code = re.sub(r'handleEncryptedMessage\(peerHandle, message\)', 'handleEncryptedMessage(message)', code)
code = re.sub(r'private fun handleEncryptedMessage\(peerHandle: PeerHandle, message: ByteArray\)', 'private fun handleEncryptedMessage(message: ByteArray)', code)

# Update broadcastMessage
broadcast_pattern = r'fun broadcastMessage.*?fun lockChannel'
new_broadcast = '''fun broadcastMessage(message: String) {
        val finalMessage = if (message.startsWith("[TTL:")) message else "[TTL:3]$message"
        val encryptedPayload = CryptoEngine.encrypt(finalMessage)
        val bytes = encryptedPayload.toByteArray()
        
        val activePeers = wifiDirectHelper?.getActivePeerCount() ?: 0
        if (activePeers == 0) {
            Log.w(TAG, "No Wi-Fi peers discovered. Falling back to Bluetooth.")
            bluetoothFallback?.broadcastFallback(finalMessage)
            return
        }
        
        sendChunked(bytes)
    }

    private fun sendChunked(fullPayload: ByteArray) {
        val chunkSize = 200
        if (fullPayload.size <= chunkSize) {
            wifiDirectHelper?.broadcastMessage(fullPayload)
            return
        }
        val chunks = fullPayload.toList().chunked(chunkSize)
        val totalChunks = chunks.size
        val messageId = (Math.random() * 1000).toInt()
        chunks.forEachIndexed { index, chunk ->
            val header = "[CHK:$messageId:$index/$totalChunks]".toByteArray()
            val packet = header + chunk.toByteArray()
            wifiDirectHelper?.broadcastMessage(packet)
        }
    }
    
    fun lockChannel'''
code = re.sub(broadcast_pattern, new_broadcast, code, flags=re.DOTALL | re.MULTILINE)

# Replace sendRawMessage internally (actually we removed it and used wifiDirectHelper directly in sendChunked and handleEncryptedMessage)
code = re.sub(r'sendRawMessage\(peerHandle, CryptoEngine\.encrypt\(syncMsg\)\.toByteArray\(\)\)', 'wifiDirectHelper?.broadcastMessage(CryptoEngine.encrypt(syncMsg).toByteArray())', code)
code = re.sub(r'sendRawMessage\(peerHandle, CryptoEngine\.encrypt\(syncRes\)\.toByteArray\(\)\)', 'wifiDirectHelper?.broadcastMessage(CryptoEngine.encrypt(syncRes).toByteArray())', code)

# Remove the old sendRawMessage block entirely
code = re.sub(r'private fun sendRawMessage.*?fun stopMesh', 'fun stopMesh', code, flags=re.DOTALL | re.MULTILINE)

# Update stopMesh
stop_pattern = r'fun stopMesh\(\) \{.*?\}'
new_stop = '''fun stopMesh() {
        wifiDirectHelper?.stop()
        wifiDirectHelper = null
        heartbeatJob?.cancel()
        Log.d(TAG, "Mesh disconnected.")
    }'''
code = re.sub(stop_pattern, new_stop, code, flags=re.DOTALL)

# Delete discoveredPeers logic
code = re.sub(r'private val discoveredPeers.*?\n', '', code)

with open('app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt', 'w', encoding='utf-8') as f:
    f.write(code)
