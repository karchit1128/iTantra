package com.example.itantra.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.itantra.R
import com.example.itantra.data.BatteryMonitorService
import com.example.itantra.mesh.MeshNetworkManager
import com.example.itantra.ml.STTEngine

class MeshForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "MeshNetworkChannel"
        private const val NOTIFICATION_ID = 173
        private const val TAG = "MeshForegroundService"
    }

    private var meshManager: MeshNetworkManager? = null
    private var batteryMonitor: BatteryMonitorService? = null

    // BUG-11 Fix: Use a scoped coroutine tied to the Service lifecycle instead of GlobalScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentTtsJob: kotlinx.coroutines.Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.w(TAG, "Screen Off: Suspending ONNX STT Engine to save power.")
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen On: Waking up ONNX STT Engine.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        meshManager = MeshNetworkManager.getInstance(this)
        batteryMonitor = BatteryMonitorService(this, meshManager!!)
        batteryMonitor?.startMonitoring()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        // BUG-11 Fix: serviceScope is cancelled in onDestroy — no more GlobalScope leak
        serviceScope.launch {
            meshManager?.incomingMessages?.collect { message ->
                if (message.startsWith("[TTS]")) {
                    var msgLang = "English"
                    var cleanMessage = message.removePrefix("[TTS]")

                    // Parse language tag: [TTS][Hindi]...
                    if (cleanMessage.startsWith("[")) {
                        val end = cleanMessage.indexOf("]")
                        if (end != -1) {
                            val tag = cleanMessage.substring(1, end)
                            // Only treat as language tag if it's not a control tag
                            if (!tag.startsWith("RED") && !tag.startsWith("ID:") && !tag.startsWith("PRIO:")) {
                                msgLang = tag
                                cleanMessage = cleanMessage.substring(end + 1)
                            }
                        }
                    }

                    // BUG-13 Fix: Parse [RED] tag properly to determine alert state
                    val isAlert = cleanMessage.startsWith("[RED]")
                    if (isAlert) {
                        cleanMessage = cleanMessage.removePrefix("[RED]")
                    }

                    // Set TTS engine language to sender's language
                    com.example.itantra.ml.TTSEngine.currentLang = msgLang

                    val prefs = getSharedPreferences("itnt_settings", Context.MODE_PRIVATE)
                    val isWalkieTalkieOn = prefs.getBoolean("mesh_visible", false)

                    // BUG-18 Fix: Play TTS if walkie-talkie is on, regardless of foreground state,
                    // since the Service itself is always running. Only notifications go to bg.
                    if (isAlert) {
                        // PREEMPTION: Cancel any ongoing TTS instantly for RED alert
                        currentTtsJob?.cancel()
                        currentTtsJob = serviceScope.launch {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, true)
                        }
                    } else if (isWalkieTalkieOn) {
                        // Normal messages queue sequentially (Wait for current to finish if it's playing)
                        if (currentTtsJob?.isActive == true && !isAlert) {
                            // Let it wait in the single-thread dispatcher queue
                        }
                        currentTtsJob = serviceScope.launch {
                            com.example.itantra.ml.TTSEngine.synthesizeAndPlay(cleanMessage, this@MeshForegroundService, false)
                        }
                    } else {
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val cleanText = cleanMessage
                            .replace(Regex("\\[ID:[^\\]]+\\]"), "")
                            .replace(Regex("\\[PRIO:[^\\]]+\\]"), "")
                            .trim()

                        val notif = NotificationCompat.Builder(this@MeshForegroundService, CHANNEL_ID)
                            .setContentTitle("New Offline Message")
                            .setContentText(cleanText)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .build()

                        try {
                            notificationManager.notify((1000..9999).random(), notif)
                        } catch (e: SecurityException) {
                            android.util.Log.e("MeshService", "Missing POST_NOTIFICATIONS permission", e)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // BUG-11 Fix: Cancel all coroutines when service is destroyed
        serviceScope.cancel()
        com.example.itantra.ml.WakeWordEngine.stopListening()
        batteryMonitor?.stopMonitoring()
        unregisterReceiver(screenReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        com.example.itantra.ml.WakeWordEngine.startListening(this) {
            Log.d("WakeWord", "Emergency triggered hands-free! Sending SOS...")
            com.example.itantra.mesh.SOSManager.sendSOS(this, "Wake Word Emergency Trigger")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iTantra Mesh Active")
            .setContentText("Mesh is active. Tap to open iTantra.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mesh Network Daemon"
            val descriptionText = "Keeps the Walkie-Talkie connected in the background"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
