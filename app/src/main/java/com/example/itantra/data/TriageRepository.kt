package com.example.itantra.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TriageRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val triageDao = database.triageDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Auto-Purge cutoff (48 hours)
    private val PURGE_CUTOFF_MS = 48 * 60 * 60 * 1000L

    val victims: StateFlow<List<TriageEntity>> = triageDao.getAllVictims()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        // Start TTL Auto-Purge Daemon
        startAutoPurge()
    }

    private fun startAutoPurge() {
        scope.launch {
            while (true) {
                try {
                    val cutoff = System.currentTimeMillis() - PURGE_CUTOFF_MS
                    triageDao.deleteOlderThan(cutoff)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Check every hour
                delay(60 * 60 * 1000L)
            }
        }
    }

    suspend fun victimExists(msgId: String): Boolean {
        return triageDao.victimExists(msgId) > 0
    }

    suspend fun insertVictim(victim: TriageEntity) {
        triageDao.insertVictim(victim)
    }
    
    suspend fun insertVictims(victims: List<TriageEntity>) {
        triageDao.insertVictims(victims)
    }
    
    suspend fun getLatestTimestamp(): Long {
        return triageDao.getLatestTimestamp() ?: 0L
    }
    
    suspend fun getVictimsSince(timestamp: Long): List<TriageEntity> {
        return triageDao.getVictimsSince(timestamp)
    }
    
    suspend fun deleteAllVictims() {
        triageDao.deleteAll()
    }

    suspend fun markAsAcked(msgId: String) {
        triageDao.markAsAcked(msgId)
    }

    companion object {
        @Volatile
        private var INSTANCE: TriageRepository? = null

        fun getInstance(context: Context): TriageRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TriageRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
