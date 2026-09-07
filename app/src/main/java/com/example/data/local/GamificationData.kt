package com.example.data.local

import androidx.compose.ui.graphics.vector.ImageVector

enum class ChallengeType {
    STEPS, CALORIES, DISTANCE, WORKOUT
}

data class DailyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val target: Float,
    val current: Float,
    val type: ChallengeType,
    val isCompleted: Boolean = current >= target,
    val isClaimed: Boolean = false,
    val rewardWater: Int = 15 // Drops of water reward
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Float,
    val currentValue: Float,
    val isUnlocked: Boolean = currentValue >= targetValue,
    val iconName: String, // e.g. "directions_run", "local_fire_department", "emoji_events", "spa", "stars", "hiking"
    val rewardXp: Int = 100
)

data class PlantState(
    val level: Int = 1,
    val currentExp: Int = 0,
    val maxExp: Int = 100,
    val totalWaterEarned: Int = 10, // Starts with some drops of water
    val totalWaterUsed: Int = 0
) {
    val progressFraction: Float get() = currentExp.toFloat() / maxExp.toFloat()

    val plantName: String get() = when (level) {
        1 -> "Sproutling"
        2 -> "Little Seedling"
        3 -> "Budding Clover"
        4 -> "Flowering Fern"
        5 -> "Golden Bloom"
        else -> "Mighty Oak Tree"
    }

    val phaseDescription: String get() = when (level) {
        1 -> "A tiny sprout emerging from the soil. Needs love and steps!"
        2 -> "A healthy green shoot with a couple of strong leaves."
        3 -> "Developing a tight bud, getting ready to flower!"
        4 -> "Beautiful leaves spread wide, absorbing all your energy."
        5 -> "Blooming with vibrant blossoms from your consistent steps."
        else -> "A legendary, fully grown forest guardian tree!"
    }
}

data class JourneyNode(
    val id: String,
    val name: String,
    val description: String,
    val targetSteps: Int,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val isCurrent: Boolean = false
)

data class InAppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String // "ACHIEVEMENT", "CHALLENGE", "PLANT", "GENERAL"
)
