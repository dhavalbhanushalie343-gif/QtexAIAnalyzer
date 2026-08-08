package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val asset: String,
    val price: Double,
    val timeframe: String,
    val signalType: String, // "UP", "DOWN", "WAIT"
    val confidence: Int,
    val reason: String,
    val dataQuality: String,
    val userResult: String = "PENDING" // "PENDING", "WIN", "LOSS", "SKIPPED"
)
