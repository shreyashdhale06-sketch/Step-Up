package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walking_sessions")
data class WalkingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long, // timestamp
    val durationSeconds: Long,
    val steps: Int,
    val calories: Double,
    val distanceKm: Double
)
