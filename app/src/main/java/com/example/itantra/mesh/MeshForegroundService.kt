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

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.w(TAG, "Screen Off: Suspending ONNX STT Engine to save power.")
                    // suspended
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen On: Waking up ONNX STT Engine.")
                    // resumed
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
        
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            meshManager?.incomingMessages?.collect { message ->
                if (message.startsWith("[TTS]")) {
                    val prefs = getSharedPreferences("itnt_settings", Context.MODE_PRIVATE)
                    val isWalkieTalkieOn = prefs.getBoolean("mesh_visible", false)
                    val isAlert = message.contains("RED") || message.contains("Priority RED") || message.contains("ALERT")
                    
                    if (isAlert) {
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
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val cleanText = message.removePrefix("[TTS]").replace(Regex("\\[ID:[^\\]]+\\]"), "").replace(Regex("\\[PRIO:[^\\]]+\\]"), "")
                        
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

        // The service prevents the system from killing the app process
        // while the user has the screen locked in their pocket.
        
        com.example.itantra.ml.WakeWordEngine.startListening(this) {
            Log.d("WakeWord", "Emergency triggered hands-free! Sending SOS...")
            com.example.itantra.mesh.SOSManager.sendSOS(this, "Wake Word Emergency Trigger")
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need bound service for now, just started
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iTantra Mesh Active")
            .setContentText("Mesh is active. Tap to open iTantra.")
            // Assuming there's an ic_launcher, using a built-in fallback if needed, but R.mipmap.ic_launcher should exist
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
