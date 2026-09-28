package com.example.itantra.hardware

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object TelecomFallbackManager {
    private const val TAG = "TelecomFallbackManager"

    fun initiateFallbackCall(context: Context, peerNumber: String) {
        try {
            Log.d(TAG, "Mesh network deemed fully degraded. Falling back to GSM cellular.")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$peerNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Telecom Manager", e)
        }
    }
}
