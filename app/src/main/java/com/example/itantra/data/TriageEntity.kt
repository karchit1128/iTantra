package com.example.itantra.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(tableName = "victims")
data class TriageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "priority") val priority: String, // RED, YELLOW, GREEN
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "latitude") val latitude: Double = 0.0,
    @ColumnInfo(name = "longitude") val longitude: Double = 0.0,
    @ColumnInfo(name = "isSentByMe") val isSentByMe: Boolean = false,
    @ColumnInfo(name = "isAcked") val isAcked: Boolean = false
)
