package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.DailyStepRecord
import com.example.data.local.WalkingSession
import com.example.data.local.UserPreferences
import kotlinx.coroutines.flow.Flow

class StepRepository(
    private val database: AppDatabase,
    private val userPreferences: UserPreferences
) {
    private val dailyStepDao = database.dailyStepDao()
    private val walkingSessionDao = database.walkingSessionDao()

    // Preferences
    val isLoggedIn: Flow<Boolean> = userPreferences.isLoggedIn
    val userName: Flow<String> = userPreferences.userName
    val dailyStepGoal: Flow<Int> = userPreferences.dailyStepGoal
    val userWeight: Flow<Float> = userPreferences.userWeight
    val userHeight: Flow<Float> = userPreferences.userHeight
    val userStrideLength: Flow<Float> = userPreferences.userStrideLength
    val currentStreak: Flow<Int> = userPreferences.currentStreak
    val useMetric: Flow<Boolean> = userPreferences.useMetric
    val darkTheme: Flow<Boolean?> = userPreferences.darkTheme
    val customGeminiApiKey: Flow<String> = userPreferences.customGeminiApiKey

    // Gamification Preferences
    val plantLevel: Flow<Int> = userPreferences.plantLevel
    val plantExp: Flow<Int> = userPreferences.plantExp
    val plantWaterEarned: Flow<Int> = userPreferences.plantWaterEarned
    val plantWaterUsed: Flow<Int> = userPreferences.plantWaterUsed
    val unlockedAchievements: Flow<String> = userPreferences.unlockedAchievements
    val claimedChallengesDate: Flow<String> = userPreferences.claimedChallengesDate
    val claimedChallengesIds: Flow<String> = userPreferences.claimedChallengesIds

    suspend fun savePlantState(level: Int, exp: Int, earned: Int, used: Int) {
        userPreferences.savePlantState(level, exp, earned, used)
    }

    suspend fun saveUnlockedAchievements(ids: String) {
        userPreferences.saveUnlockedAchievements(ids)
    }

    suspend fun saveClaimedChallenges(date: String, ids: String) {
        userPreferences.saveClaimedChallenges(date, ids)
    }

    suspend fun setLoggedIn(loggedIn: Boolean) = userPreferences.setLoggedIn(loggedIn)
    suspend fun saveProfile(name: String, goal: Int, weight: Float, height: Float, strideLength: Float = height * 0.414f) =
        userPreferences.saveProfile(name, goal, weight, height, strideLength)
    suspend fun updateStreak(streak: Int) = userPreferences.updateStreak(streak)
    suspend fun setUseMetric(metric: Boolean) = userPreferences.setUseMetric(metric)
    suspend fun setDarkTheme(dark: Boolean?) = userPreferences.setDarkTheme(dark)
    suspend fun setCustomGeminiApiKey(apiKey: String) = userPreferences.setCustomGeminiApiKey(apiKey)

    // Daily Steps Database Operations
    val allDailyRecords: Flow<List<DailyStepRecord>> = dailyStepDao.getAllRecords()

    suspend fun getTodayRecord(dateStr: String): DailyStepRecord? {
        return dailyStepDao.getRecordByDate(dateStr)
    }

    suspend fun insertDailyRecord(record: DailyStepRecord) {
        dailyStepDao.insertRecord(record)
    }

    // Walking Sessions Database Operations
    val allWalkingSessions: Flow<List<WalkingSession>> = walkingSessionDao.getAllSessions()

    suspend fun insertWalkingSession(session: WalkingSession) {
        walkingSessionDao.insertSession(session)
    }

    suspend fun deleteWalkingSession(session: WalkingSession) {
        walkingSessionDao.deleteSession(session)
    }

    suspend fun clearAllData() {
        dailyStepDao.deleteAllRecords()
        walkingSessionDao.deleteAllSessions()
    }
}
