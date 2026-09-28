package com.example.itantra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TriageDao {
    @Query("SELECT COUNT(*) FROM victims WHERE id = :msgId")
    suspend fun victimExists(msgId: String): Int

    @Query("SELECT * FROM victims ORDER BY timestamp DESC")
    fun getAllVictims(): Flow<List<TriageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVictim(victim: TriageEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVictims(victims: List<TriageEntity>)

    // TTL Auto-Purge: Delete victims older than the cutoff timestamp
    @Query("DELETE FROM victims WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
    
    // For Gossip Protocol Sync
    @Query("SELECT MAX(timestamp) FROM victims")
    suspend fun getLatestTimestamp(): Long?
    
    @Query("SELECT * FROM victims WHERE timestamp > :sinceTimestamp")
    suspend fun getVictimsSince(sinceTimestamp: Long): List<TriageEntity>
    
    // For Panic Wipe Kill Switch
    @Query("DELETE FROM victims")
    suspend fun deleteAll()

    @Query("UPDATE victims SET isAcked = 1 WHERE id = :msgId")
    suspend fun markAsAcked(msgId: String)

    @Query("SELECT isAcked FROM victims WHERE id = :msgId")
    suspend fun isAcked(msgId: String): Boolean?

    @Query("DELETE FROM victims")
    suspend fun clearAll()

}
