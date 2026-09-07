package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_step_records")
data class DailyStepRecord(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val steps: Int,
    val goal: Int,
    val calories: Int,
    val distanceKm: Double,
    val walkingTimeMinutes: Int
)
