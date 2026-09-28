package com.example.itantra.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.itantra.data.TriageEntity
import com.example.itantra.data.TriageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class ChannelState(val isLocked: Boolean, val lockedBy: String = "")

class MeshNetworkManager(private val context: Context) {
    companion object {
        private const val TAG = "iTantraMesh"
        private const val SERVICE_NAME = "iTantra-SOS-Mesh"
        private const val WPA2_PASSPHRASE = "iTantraRescue173!"

        @Volatile
        private var INSTANCE: MeshNetworkManager? = null

        fun getInstance(context: Context): MeshNetworkManager {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: MeshNetworkManager(context.applicationContext).also { INSTANCE = it }
                instance
            }
        }
    }

    private var wifiDirectHelper: WifiDirectHelper? = null
    private val _discoveredPeersState = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val discoveredPeersState: StateFlow<List<Pair<String, String>>> = _discoveredPeersState

    private val _connectedPeersCount = MutableStateFlow(0)
    val connectedPeersCount: StateFlow<Int> = _connectedPeersCount

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val incomingMessages: SharedFlow<String> = _incomingMessages
    
    // Half-Duplex Channel State
    private val _channelState = MutableStateFlow(ChannelState(false))
    val channelState: StateFlow<ChannelState> = _channelState
    
    private var heartbeatIntervalMs = 5000L // 5 seconds default
    private var heartbeatJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val triageRepo = TriageRepository.getInstance(context)
    
    private val chunkBuffers = java.util.concurrent.ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()
    
    private var bluetoothFallback: BluetoothFallbackManager? = null

    @SuppressLint("MissingPermission")
    fun startMesh() {
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
            wifiDirectHelper?.onPeersChanged = { peers ->
                _discoveredPeersState.value = peers
            }
            wifiDirectHelper?.onConnectionChanged = { count ->
                _connectedPeersCount.value = count
            }
        }
        wifiDirectHelper?.start()
        
        startAdaptiveHeartbeat()
    }
    
    fun connectToPeer(address: String) {
        wifiDirectHelper?.connectToPeer(address)
    }

    private fun processReceivedBytes(message: ByteArray) {
        val peekLen = minOf(message.size, 30)
        val headerStr = String(message.copyOfRange(0, peekLen))
        if (headerStr.startsWith("[CHK:")) {
            val endIdx = message.indexOf(']'.code.toByte())
            if (endIdx != -1) {
                val header = String(message.copyOfRange(0, endIdx + 1))
                val payload = message.copyOfRange(endIdx + 1, message.size)
                
                val parts = header.removePrefix("[CHK:").removeSuffix("]").split(":")
                if (parts.size == 2) {
                    val msgId = parts[0]
                    val indexParts = parts[1].split("/")
                    if (indexParts.size == 2) {
                        val idx = indexParts[0].toIntOrNull() ?: 0
                        val total = indexParts[1].toIntOrNull() ?: 1
                        
                        val map = chunkBuffers.getOrPut(msgId) { java.util.concurrent.ConcurrentHashMap() }
                        map[idx] = payload
                        
                        if (map.size == total) {
                            var totalSize = 0
                            for (i in 0 until total) totalSize += (map[i]?.size ?: 0)
                            
                            val fullMessage = ByteArray(totalSize)
                            var offset = 0
                            for (i in 0 until total) {
                                val chunk = map[i] ?: ByteArray(0)
                                System.arraycopy(chunk, 0, fullMessage, offset, chunk.size)
                                offset += chunk.size
                            }
                            chunkBuffers.remove(msgId)
                            handleEncryptedMessage(fullMessage)
                        }
                        return
                    }
                }
            }
        }
        // If not a chunk, handle normally
        handleEncryptedMessage(message)
    }

    private fun handleEncryptedMessage(message: ByteArray) {
        val encryptedText = String(message)
        val plainTextRaw = CryptoEngine.decrypt(encryptedText)
        
        if (plainTextRaw.isEmpty()) {
            Log.e(TAG, "Failed to decrypt message from peer.")
            return
        }
        
        if (!plainTextRaw.startsWith("[PING]") && !plainTextRaw.startsWith("[ACK:")) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                // Toast removed for production
            }
        }
        
        Log.d(TAG, "Decrypted message: $plainTextRaw")

        var plainText = plainTextRaw
        var currentTtl = 0
        if (plainText.startsWith("[TTL:")) {
            val closingBracketIndex = plainText.indexOf("]")
            if (closingBracketIndex != -1) {
                currentTtl = plainText.substring(5, closingBracketIndex).toIntOrNull() ?: 0
                plainText = plainText.substring(closingBracketIndex + 1)
            }
        }
        
        // Handle Gossip Sync Protocol
        if (plainText.startsWith("[SYNC_REQ]")) {
            val timestamp = plainText.removePrefix("[SYNC_REQ] ").toLongOrNull() ?: 0L
            scope.launch {
                val missingRecords = triageRepo.getVictimsSince(timestamp)
                if (missingRecords.isNotEmpty()) {
                    val payload = missingRecords.joinToString(";") {
                        "${it.id}|${it.priority}|${it.timestamp}|${it.latitude}|${it.longitude}|${it.message}"
                    }
                    val syncRes = "[SYNC_RES] $payload"
                    wifiDirectHelper?.broadcastMessage(CryptoEngine.encrypt(syncRes).toByteArray())
                }
            }
        } else if (plainText.startsWith("[SYNC_RES]")) {
            val payload = plainText.removePrefix("[SYNC_RES] ")
            try {
                val newEntities = mutableListOf<TriageEntity>()
                val records = payload.split(";")
                for (record in records) {
                    if (record.isBlank()) continue
                    val parts = record.split("|", limit = 6)
                    if (parts.size == 6) {
                        newEntities.add(TriageEntity(
                            id = parts[0],
                            priority = parts[1],
                            timestamp = parts[2].toLongOrNull() ?: 0L,
                            latitude = parts[3].toDoubleOrNull() ?: 0.0,
                            longitude = parts[4].toDoubleOrNull() ?: 0.0,
                            message = parts[5]
                        ))
                    }
                }
                scope.launch {
                    triageRepo.insertVictims(newEntities)
                    Log.d(TAG, "Gossip Sync: Imported ${newEntities.size} missing records.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SYNC_RES", e)
            }
        } else if (plainText.startsWith("[PING]")) {
            Log.d(TAG, "Received heartbeat.")
        } else if (plainText.startsWith("[LOCK:]")) {
            val sender = plainText.removePrefix("[LOCK:]")
            _channelState.value = ChannelState(true, sender)
        } else if (plainText == "[UNLOCK]") {
            _channelState.value = ChannelState(false, "")
        } else if (plainText.startsWith("[ACK:")) {
            val msgId = plainText.substringAfter("[ACK:").substringBefore("]")
            scope.launch {
                triageRepo.markAsAcked(msgId)
            }
        } else {
            scope.launch {
                var msgId = ""
                var msgPriority = "YELLOW"
                var actualMessageInner = plainText
                if (actualMessageInner.startsWith("[ID:")) {
                    val idEnd = actualMessageInner.indexOf("]")
                    if (idEnd != -1) {
                        msgId = actualMessageInner.substring(4, idEnd)
                        actualMessageInner = actualMessageInner.substring(idEnd + 1)
                        
                        if (triageRepo.victimExists(msgId)) {
                            return@launch
                        }
                        
                        broadcastMessage("[ACK:$msgId]")
                    }
                }
                
                if (actualMessageInner.startsWith("[PRIO:")) {
                    val pEnd = actualMessageInner.indexOf("]")
                    if (pEnd != -1) {
                        msgPriority = actualMessageInner.substring(6, pEnd)
                        actualMessageInner = actualMessageInner.substring(pEnd + 1)
                    }
                }
                
                if (actualMessageInner.startsWith("[ACK:")) {
                    val ackedId = actualMessageInner.substringAfter("[ACK:").substringBefore("]")
                    triageRepo.markAsAcked(ackedId)
                    return@launch
                }
                
                val rawMsg = actualMessageInner.removePrefix("[TTS]")
                val prefs = context.getSharedPreferences("itnt_settings", android.content.Context.MODE_PRIVATE)
                val targetLang = prefs.getString("target_language", "English") ?: "English"
                
                var translatedMsg = rawMsg
                try {
                    translatedMsg = com.example.itantra.ml.TranslationEngine.translate(rawMsg, "Unknown", targetLang)
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed", e)
                }
                
                val entity = TriageEntity(id = if(msgId.isNotEmpty()) msgId else java.util.UUID.randomUUID().toString(), message = translatedMsg, priority = msgPriority, isSentByMe = false)
                triageRepo.insertVictim(entity)
                _incomingMessages.tryEmit(if(actualMessageInner.startsWith("[TTS]")) "[TTS]$translatedMsg" else translatedMsg)
                
                if (currentTtl > 0) {
                    val newTtl = currentTtl - 1
                    delay(500)
                    // preserve original payload but decrement TTL
                    val prioTag = if (msgPriority != "YELLOW") "[PRIO:$msgPriority]" else ""
                    broadcastMessage("[TTL:$newTtl][ID:$msgId]$prioTag$actualMessageInner")
                }
                
                if (actualMessageInner.contains("SOS", ignoreCase = true) || actualMessageInner.contains("DEATH RATTLE", ignoreCase = true)) {
                    com.example.itantra.hardware.FlashlightManager.strobeSos(context)
                }
            }
        }
    }

    fun broadcastMessage(message: String) {
        val finalMessage = if (message.startsWith("[TTL:")) message else "[TTL:3]$message"
        val encryptedPayload = CryptoEngine.encrypt(finalMessage)
        val bytes = encryptedPayload.toByteArray()
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            // Toast removed for production
        }
        
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
    
    fun lockChannel(myId: String) {
        broadcastMessage("[LOCK:]$myId")
    }
    
    fun unlockChannel() {
        broadcastMessage("[UNLOCK]")
    }
    
    fun setBatteryLevel(level: Int) {
        val newInterval = if (level < 15) {
            Log.w(TAG, "Low battery ($level%). Throttling heartbeat to 30s.")
            30000L
        } else {
            5000L
        }
        if (newInterval != heartbeatIntervalMs) {
            heartbeatIntervalMs = newInterval
            if (wifiDirectHelper != null) {
                startAdaptiveHeartbeat() // Restart the loop immediately
            }
        }
    }
    
    private fun startAdaptiveHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (wifiDirectHelper != null) {
                delay(heartbeatIntervalMs)
                broadcastMessage("[PING]")
            }
        }
    }
    
    fun stopMesh() {
        wifiDirectHelper?.stop()
        wifiDirectHelper = null
        heartbeatJob?.cancel()
        Log.d(TAG, "Mesh disconnected.")
    }
}
