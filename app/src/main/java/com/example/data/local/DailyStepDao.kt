package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepDao {
    @Query("SELECT * FROM daily_step_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyStepRecord>>

    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): DailyStepRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyStepRecord)

    @Query("DELETE FROM daily_step_records")
    suspend fun deleteAllRecords()
}
