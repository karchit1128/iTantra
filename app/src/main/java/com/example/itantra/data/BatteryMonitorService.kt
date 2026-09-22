package com.example.itantra.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.itantra.mesh.MeshNetworkManager
import kotlinx.coroutines.launch

class BatteryMonitorService(
    private val context: Context,
    private val meshManager: MeshNetworkManager
) {
    companion object {
        private const val TAG = "BatteryMonitor"
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                
                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100) / scale.toFloat()
                    val pct = batteryPct.toInt()
                    Log.d(TAG, "Battery level changed: $pct%")
                    meshManager.setBatteryLevel(pct)
                    
                    if (pct <= 1) {
                        Log.e(TAG, "CRITICAL BATTERY - FIRING DEATH RATTLE PING")
                        meshManager.broadcastMessage("[TTS]Alert: Node going offline due to critical battery.")
                        // We rely on LocationEngine.lastLat as this is a background service and might not await quickly
                        val lat = com.example.itantra.hardware.LocationEngine.lastLat
                        val lng = com.example.itantra.hardware.LocationEngine.lastLng
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val repo = com.example.itantra.data.TriageRepository.getInstance(context!!)
                            repo.insertVictim(com.example.itantra.data.TriageEntity(
                                priority = "RED",
                                message = "DEATH RATTLE: Battery 1%. Node shutting down.",
                                latitude = lat,
                                longitude = lng
                            ))
                        }
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        Log.d(TAG, "Started monitoring battery for adaptive heartbeat.")
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
            Log.d(TAG, "Stopped monitoring battery.")
        } catch (e: Exception) {
            Log.e(TAG, "Receiver not registered", e)
        }
    }
}
