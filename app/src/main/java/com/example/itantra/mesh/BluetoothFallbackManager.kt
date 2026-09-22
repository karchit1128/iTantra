package com.example.itantra.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.UUID

class BluetoothFallbackManager(private val context: Context, private val onMessageReceived: (String) -> Unit) {
    companion object {
        private const val TAG = "iTantraBTFallback"
        private const val APP_NAME = "iTantraRescue"
        // Standard SPP UUID
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private val activeSockets: MutableList<BluetoothSocket> = Collections.synchronizedList(mutableListOf())
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not available or not enabled.")
            return
        }

        scope.launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
                Log.d(TAG, "Bluetooth Fallback Server listening...")

                while (true) {
                    val socket = serverSocket?.accept()
                    socket?.let {
                        Log.d(TAG, "Bluetooth Fallback connected to peer: ${it.remoteDevice.name}")
                        activeSockets.add(it)
                        listenForData(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth server socket failed", e)
            }
        }
    }

    private fun listenForData(socket: BluetoothSocket) {
        scope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                val buffer = ByteArray(4096)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val encryptedMessage = String(buffer, 0, bytes)
                        // Decrypt before sending upstream
                        val decrypted = CryptoEngine.decrypt(encryptedMessage)
                        if (decrypted.isNotEmpty()) {
                            onMessageReceived(decrypted)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connection lost", e)
                activeSockets.remove(socket)
                socket.close()
            }
        }
    }

    fun broadcastFallback(message: String) {
        if (activeSockets.isEmpty()) return
        
        val encryptedPayload = CryptoEngine.encrypt(message)
        val bytes = encryptedPayload.toByteArray()
        
        // Bug 13b: Take snapshot before iterating to prevent ConcurrentModificationException
        val snapshot = synchronized(activeSockets) { activeSockets.toList() }
        snapshot.forEach { socket ->
            try {
                val outStream: OutputStream = socket.outputStream
                outStream.write(bytes)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send fallback message", e)
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            activeSockets.forEach { it.close() }
            activeSockets.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Bluetooth sockets", e)
        }
    }
}
