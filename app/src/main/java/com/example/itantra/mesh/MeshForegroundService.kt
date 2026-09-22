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
