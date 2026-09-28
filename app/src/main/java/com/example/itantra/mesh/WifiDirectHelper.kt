package com.example.itantra.mesh

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

class WifiDirectHelper(private val context: Context, private val onMessageReceived: (ByteArray) -> Unit) {
    companion object {
        private const val TAG = "iTantraWifiDirect"
        private const val PORT = 8988
    }

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)
    private var receiver: BroadcastReceiver? = null

    private val activeSockets = Collections.synchronizedList(mutableListOf<Socket>())
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    var onPeersChanged: ((List<Pair<String, String>>) -> Unit)? = null
    var onConnectionChanged: ((Int) -> Unit)? = null
    private var isStarted = false

    @SuppressLint("MissingPermission")
    fun connectToPeer(address: String) {
        val config = WifiP2pConfig().apply {
            deviceAddress = address
            groupOwnerIntent = 15 // BUG-17 Fix: Force determinism
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Toast removed
            }
            override fun onFailure(reason: Int) {
                // Toast removed
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (manager == null || channel == null) {
            Log.e(TAG, "Wi-Fi Direct not supported")
            return
        }

        if (!isStarted) {
            // Check if already connected from a previous session
            manager.requestConnectionInfo(channel) { info ->
                if (info.groupFormed && info.isGroupOwner) {
                    startServer()
                } else if (info.groupFormed) {
                    startClient(info.groupOwnerAddress.hostAddress ?: "")
                }
            }

            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        manager.requestPeers(channel) { peers ->
                            val peerList = peers.deviceList.map { Pair(it.deviceName ?: "Unknown", it.deviceAddress ?: "") }
                            onPeersChanged?.invoke(peerList)
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val networkInfo: android.net.NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            manager.requestConnectionInfo(channel) { info ->
                                if (info.groupFormed && info.isGroupOwner) {
                                    startServer()
                                } else if (info.groupFormed) {
                                    startClient(info.groupOwnerAddress.hostAddress ?: "")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        context.registerReceiver(receiver, intentFilter)
        isStarted = true
        }
        
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Toast removed
            }
            override fun onFailure(reason: Int) {
                // Toast removed
            }
        })
    }

    private fun startServer() {
        // Toast removed
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server started on port $PORT")
                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected")
                    // Toast removed
                    activeSockets.add(socket)
                    onConnectionChanged?.invoke(activeSockets.size)
                    listenForData(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket failed", e)
            }
        }
    }

    private fun startClient(host: String) {
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
    }

    private fun listenForData(socket: Socket) {
        scope.launch { listenForDataSync(socket) }
    }

    fun broadcastMessage(bytes: ByteArray) {
        val snapshot = synchronized(activeSockets) { activeSockets.toList() }
        snapshot.forEach { socket ->
            try {
                synchronized(socket) {
                    val outStream = java.io.DataOutputStream(socket.getOutputStream())
                    outStream.writeInt(bytes.size)
                    outStream.write(bytes)
                    outStream.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }
    
    fun getActivePeerCount(): Int = activeSockets.size

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            manager?.requestGroupInfo(channel) { group ->
                if (group != null) {
                    manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() { Log.d(TAG, "Group removed") }
                        override fun onFailure(reason: Int) { Log.d(TAG, "Failed to remove group: $reason") }
                    })
                }
            }
            receiver?.let { context.unregisterReceiver(it) }
            serverSocket?.close()
            activeSockets.forEach { it.close() }
            activeSockets.clear()
            onConnectionChanged?.invoke(0)
            isStarted = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WifiDirectHelper", e)
        }
    }
}
