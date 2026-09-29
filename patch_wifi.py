import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/mesh/WifiDirectHelper.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''    private fun startClient(host: String) {
        // Toast removed
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, PORT), 5000)
                Log.d(TAG, "Client connected to $host")
                // Toast removed
                activeSockets.add(socket)
                onConnectionChanged?.invoke(activeSockets.size)
                listenForData(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Client connect failed", e)
                // Toast removed
            }
        }
    }'''

replacement = '''    private fun startClient(host: String) {
        scope.launch {
            while (isStarted) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, PORT), 5000)
                    Log.d(TAG, "Client connected to $host")
                    activeSockets.add(socket)
                    onConnectionChanged?.invoke(activeSockets.size)
                    listenForDataSync(socket) // Use synchronous listening so the loop pauses here
                } catch (e: Exception) {
                    Log.e(TAG, "Client connect failed or lost, retrying in 3s...", e)
                }
                kotlinx.coroutines.delay(3000) // Wait before retrying
            }
        }
    }
    
    private fun listenForDataSync(socket: Socket) {
        try {
            val inputStream = java.io.DataInputStream(socket.getInputStream())
            while (true) {
                val length = inputStream.readInt()
                if (length > 0) {
                    val buffer = ByteArray(length)
                    inputStream.readFully(buffer)
                    onMessageReceived(buffer)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection lost", e)
        } finally {
            activeSockets.remove(socket)
            onConnectionChanged?.invoke(activeSockets.size)
            socket.close()
        }
    }'''

target2 = '''    private fun listenForData(socket: Socket) {
        scope.launch {
            try {
                val inputStream = java.io.DataInputStream(socket.getInputStream())
                while (true) {
                    val length = inputStream.readInt()
                    if (length > 0) {
                        val buffer = ByteArray(length)
                        inputStream.readFully(buffer)
                        onMessageReceived(buffer)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket connection lost", e)
            } finally {
                activeSockets.remove(socket)
                onConnectionChanged?.invoke(activeSockets.size)
                socket.close()
            }
        }
    }'''

replacement2 = '''    private fun listenForData(socket: Socket) {
        scope.launch { listenForDataSync(socket) }
    }'''

if target in text and target2 in text:
    text = text.replace(target, replacement)
    text = text.replace(target2, replacement2)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched WifiDirectHelper successfully!')
else:
    print('Target not found in WifiDirectHelper.kt.')
