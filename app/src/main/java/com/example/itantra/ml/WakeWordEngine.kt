package com.example.itantra.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * Simulates a lightweight, always-on Wake Word engine.
 * In a full offline deployment, this would wrap something like OpenWakeWord (ONNX).
 * For this MVP, it acts as a structured wrapper that can trigger hands-free SOS.
 */
object WakeWordEngine {
    private const val TAG = "WakeWordEngine"
    private var isListening = false
    private var job: Job? = null

    fun startListening(context: Context, onWakeWordDetected: () -> Unit) {
        if (isListening) return
        isListening = true
        Log.d(TAG, "WakeWordEngine started listening for 'Emergency'...")
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive && isListening) {
                    // Simulate an ultra-low-power audio threshold loop
                    delay(5000) 
                    
                    // In real life, audio amplitude would be checked against a threshold.
                    // For demo purposes, we will not randomly trigger it to prevent chaos.
                    // But the architecture is here to plug in the ONNX wake word model.
                }
            } catch (e: Exception) {
                Log.e(TAG, "WakeWordEngine interrupted", e)
            }
        }
    }

    fun stopListening() {
        isListening = false
        job?.cancel()
        Log.d(TAG, "WakeWordEngine stopped.")
    }
}
