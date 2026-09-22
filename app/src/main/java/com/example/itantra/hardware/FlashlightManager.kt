package com.example.itantra.hardware

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object FlashlightManager {
    private const val TAG = "FlashlightManager"
    private var isStrobing = false

    fun strobeSos(context: Context) {
        if (isStrobing) return
        isStrobing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                
                if (cameraId != null) {
                    Log.d(TAG, "Starting SOS Strobe on camera $cameraId")
                    // SOS pattern: 3 short, 3 long, 3 short
                    val pattern = listOf(
                        200L, 200L, 200L, 200L, 200L, 600L, // 3 short
                        600L, 200L, 600L, 200L, 600L, 600L, // 3 long
                        200L, 200L, 200L, 200L, 200L, 600L  // 3 short
                    )
                    
                    for (duration in pattern) {
                        if (!isStrobing) break
                        cameraManager.setTorchMode(cameraId, true)
                        delay(duration)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(200L)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to use flashlight", e)
            } finally {
                isStrobing = false
            }
        }
    }
    
    fun stop() {
        isStrobing = false
    }
}
