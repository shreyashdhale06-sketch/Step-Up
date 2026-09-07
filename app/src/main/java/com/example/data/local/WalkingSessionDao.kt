package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkingSessionDao {
    @Query("SELECT * FROM walking_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WalkingSession)

    @Delete
    suspend fun deleteSession(session: WalkingSession)

    @Query("DELETE FROM walking_sessions")
    suspend fun deleteAllSessions()
}
