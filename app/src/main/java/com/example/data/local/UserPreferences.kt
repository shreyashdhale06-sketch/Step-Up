package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_NAME = stringPreferencesKey("user_name")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
        val USER_WEIGHT = floatPreferencesKey("user_weight") // in kg
        val USER_HEIGHT = floatPreferencesKey("user_height") // in cm
        val USER_STRIDE_LENGTH = floatPreferencesKey("user_stride_length") // in cm
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val USE_METRIC = booleanPreferencesKey("use_metric")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val CUSTOM_GEMINI_API_KEY = stringPreferencesKey("custom_gemini_api_key")

        // Gamification Persistence
        val PLANT_LEVEL = intPreferencesKey("plant_level")
        val PLANT_EXP = intPreferencesKey("plant_exp")
        val PLANT_WATER_EARNED = intPreferencesKey("plant_water_earned")
        val PLANT_WATER_USED = intPreferencesKey("plant_water_used")
        val UNLOCKED_ACHIEVEMENTS = stringPreferencesKey("unlocked_achievements")
        val CLAIMED_CHALLENGES_DATE = stringPreferencesKey("claimed_challenges_date")
        val CLAIMED_CHALLENGES_IDS = stringPreferencesKey("claimed_challenges_ids")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "Alex" }
    val dailyStepGoal: Flow<Int> = context.dataStore.data.map { it[DAILY_STEP_GOAL] ?: 8000 }
    val userWeight: Flow<Float> = context.dataStore.data.map { it[USER_WEIGHT] ?: 70f }
    val userHeight: Flow<Float> = context.dataStore.data.map { it[USER_HEIGHT] ?: 175f }
    val userStrideLength: Flow<Float> = context.dataStore.data.map { it[USER_STRIDE_LENGTH] ?: 72.5f }
    val currentStreak: Flow<Int> = context.dataStore.data.map { it[CURRENT_STREAK] ?: 0 }
    val useMetric: Flow<Boolean> = context.dataStore.data.map { it[USE_METRIC] ?: true }
    val darkTheme: Flow<Boolean?> = context.dataStore.data.map { it[DARK_THEME] }
    val customGeminiApiKey: Flow<String> = context.dataStore.data.map { it[CUSTOM_GEMINI_API_KEY] ?: "" }

    // Gamification Flows
    val plantLevel: Flow<Int> = context.dataStore.data.map { it[PLANT_LEVEL] ?: 1 }
    val plantExp: Flow<Int> = context.dataStore.data.map { it[PLANT_EXP] ?: 0 }
    val plantWaterEarned: Flow<Int> = context.dataStore.data.map { it[PLANT_WATER_EARNED] ?: 10 }
    val plantWaterUsed: Flow<Int> = context.dataStore.data.map { it[PLANT_WATER_USED] ?: 0 }
    val unlockedAchievements: Flow<String> = context.dataStore.data.map { it[UNLOCKED_ACHIEVEMENTS] ?: "" }
    val claimedChallengesDate: Flow<String> = context.dataStore.data.map { it[CLAIMED_CHALLENGES_DATE] ?: "" }
    val claimedChallengesIds: Flow<String> = context.dataStore.data.map { it[CLAIMED_CHALLENGES_IDS] ?: "" }

    suspend fun savePlantState(level: Int, exp: Int, earned: Int, used: Int) {
        context.dataStore.edit {
            it[PLANT_LEVEL] = level
            it[PLANT_EXP] = exp
            it[PLANT_WATER_EARNED] = earned
            it[PLANT_WATER_USED] = used
        }
    }

    suspend fun saveUnlockedAchievements(ids: String) {
        context.dataStore.edit {
            it[UNLOCKED_ACHIEVEMENTS] = ids
        }
    }

    suspend fun saveClaimedChallenges(date: String, ids: String) {
        context.dataStore.edit {
            it[CLAIMED_CHALLENGES_DATE] = date
            it[CLAIMED_CHALLENGES_IDS] = ids
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[IS_LOGGED_IN] = loggedIn }
    }

    suspend fun saveProfile(name: String, goal: Int, weight: Float, height: Float, strideLength: Float = height * 0.414f) {
        context.dataStore.edit {
            it[USER_NAME] = name
            it[DAILY_STEP_GOAL] = goal
            it[USER_WEIGHT] = weight
            it[USER_HEIGHT] = height
            it[USER_STRIDE_LENGTH] = strideLength
        }
    }

    suspend fun updateStreak(streak: Int) {
        context.dataStore.edit { it[CURRENT_STREAK] = streak }
    }

    suspend fun setUseMetric(metric: Boolean) {
        context.dataStore.edit { it[USE_METRIC] = metric }
    }

    suspend fun setDarkTheme(dark: Boolean?) {
        context.dataStore.edit {
            if (dark == null) {
                it.remove(DARK_THEME)
            } else {
                it[DARK_THEME] = dark
            }
        }
    }

    suspend fun setCustomGeminiApiKey(apiKey: String) {
        context.dataStore.edit {
            it[CUSTOM_GEMINI_API_KEY] = apiKey.trim()
        }
    }
}
