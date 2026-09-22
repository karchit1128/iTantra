package com.example.itantra.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object PanicWipeManager {
    private const val TAG = "PanicWipeManager"

    fun executeWipe(context: Context, onComplete: () -> Unit) {
        Log.w(TAG, "INITIATING PANIC WIPE...")
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            try {
                // 1. Wipe SQLite Database
                val repo = TriageRepository.getInstance(context)
                repo.deleteAllVictims()
                Log.w(TAG, "SQLite Triage Database WIPED.")

                // 2. Wipe Cache & Files (ONNX models, temporary audio, etc.)
                deleteRecursive(context.cacheDir)
                deleteRecursive(context.filesDir)
                Log.w(TAG, "Cache & Files WIPED.")
                
                // 3. Clear SharedPreferences
                context.getSharedPreferences("itnt_settings", Context.MODE_PRIVATE).edit().clear().apply()
                Log.w(TAG, "SharedPreferences WIPED.")
                
                // 4. Shutdown ML Engines
                com.example.itantra.ml.TTSEngine.shutdown()
                com.example.itantra.ml.STTEngine.shutdown()
                
            } catch (e: Exception) {
                Log.e(TAG, "Panic Wipe encountered an error", e)
            } finally {
                // Tell UI it's done so it can reset or close
                onComplete()
            }
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        // Don't delete the root cache/files dir itself, just contents
        if (fileOrDirectory.name != "cache" && fileOrDirectory.name != "files") {
            fileOrDirectory.delete()
        }
    }
}
