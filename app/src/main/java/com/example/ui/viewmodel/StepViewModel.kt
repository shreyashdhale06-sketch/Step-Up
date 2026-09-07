package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.remote.gemini.GeminiCoachService
import com.example.data.repository.StepRepository
import com.example.sensor.StepSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineFallback: Boolean = false,
    val isStreaming: Boolean = false
)

class StepViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StepRepository(
        AppDatabase.getDatabase(application),
        UserPreferences(application)
    )

    private val geminiService = GeminiCoachService()

    private val sensorTracker = StepSensorManager(application) { count ->
        recordSensorSteps(count)
    }

    // 1. Core Live Metric State Flows (Must be initialized first)
    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _todayCalories = MutableStateFlow(0)
    val todayCalories: StateFlow<Int> = _todayCalories.asStateFlow()

    private val _todayDistanceKm = MutableStateFlow(0.0)
    val todayDistanceKm: StateFlow<Double> = _todayDistanceKm.asStateFlow()

    private val _todayTimeMinutes = MutableStateFlow(0)
    val todayTimeMinutes: StateFlow<Int> = _todayTimeMinutes.asStateFlow()

    // 2. History Streams
    val dailyRecords: StateFlow<List<DailyStepRecord>> = repository.allDailyRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val walkingSessions: StateFlow<List<WalkingSession>> = repository.allWalkingSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. User Preferences Streams
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val userName: StateFlow<String> = repository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex")
    
    val dailyStepGoal: StateFlow<Int> = repository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8000)
    
    val userWeight: StateFlow<Float> = repository.userWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)
    
    val userHeight: StateFlow<Float> = repository.userHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 175f)
    
    val userStrideLength: StateFlow<Float> = repository.userStrideLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 72.5f)
    
    val currentStreak: StateFlow<Int> = repository.currentStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val useMetric: StateFlow<Boolean> = repository.useMetric
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val darkTheme: StateFlow<Boolean?> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customGeminiApiKey: StateFlow<String> = repository.customGeminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isGeminiActive: StateFlow<Boolean> = repository.customGeminiApiKey.map { custom ->
        val effective = if (custom.isNotBlank()) custom else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) { "" }
        }
        effective.isNotBlank() && effective != "MY_GEMINI_API_KEY"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // All-time highest step record
    val allTimeHighestSteps: StateFlow<Int> = combine(
        dailyRecords,
        todaySteps
    ) { records, currentToday ->
        val recordMax = records.maxOfOrNull { it.steps } ?: 0
        maxOf(recordMax, currentToday)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // AI Coach Chatbot State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! 👟 I'm your StepUp AI Fitness Coach. I can help analyze your step pace, calories burned, remaining distance, or provide personalized walking workout tips. What's on your mind today?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // 4. Gamification State Flows (Depend on core streams)
    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications: StateFlow<List<InAppNotification>> = _notifications.asStateFlow()

    val plantState: StateFlow<PlantState> = combine(
        repository.plantLevel,
        repository.plantExp,
        repository.plantWaterEarned,
        repository.plantWaterUsed
    ) { level, exp, earned, used ->
        PlantState(
            level = level,
            currentExp = exp,
            maxExp = level * 100,
            totalWaterEarned = earned,
            totalWaterUsed = used
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantState())

    val dailyChallenges: StateFlow<List<DailyChallenge>> = combine(
        todaySteps,
        todayCalories,
        walkingSessions,
        dailyStepGoal,
        repository.claimedChallengesDate,
        repository.claimedChallengesIds
    ) { arr ->
        val steps = arr[0] as Int
        val calories = arr[1] as Int
        @Suppress("UNCHECKED_CAST")
        val sessions = arr[2] as List<WalkingSession>
        val goal = arr[3] as Int
        val claimedDate = arr[4] as String
        val claimedIds = arr[5] as String

        val todayStr = getTodayDateString()
        val isClaimedToday = claimedDate == todayStr
        val claimedSet = if (isClaimedToday) claimedIds.split(",").toSet() else emptySet()
        val todaySessionsCount = sessions.count { isSameDay(it.startTime, System.currentTimeMillis()) }

        listOf(
            DailyChallenge(
                id = "challenge_steps_start",
                title = "Daily Warmup",
                description = "Record 2,500 steps today.",
                target = 2500f,
                current = steps.toFloat(),
                type = ChallengeType.STEPS,
                isCompleted = steps >= 2500,
                isClaimed = claimedSet.contains("challenge_steps_start")
            ),
            DailyChallenge(
                id = "challenge_steps_goal",
                title = "Peak Performer",
                description = "Complete your daily target of $goal steps.",
                target = goal.toFloat(),
                current = steps.toFloat(),
                type = ChallengeType.STEPS,
                isCompleted = steps >= goal,
                isClaimed = claimedSet.contains("challenge_steps_goal")
            ),
            DailyChallenge(
                id = "challenge_calories",
                title = "Energy Burner",
                description = "Burn 200 kcal from walking.",
                target = 200f,
                current = calories.toFloat(),
                type = ChallengeType.CALORIES,
                isCompleted = calories >= 200,
                isClaimed = claimedSet.contains("challenge_calories")
            ),
            DailyChallenge(
                id = "challenge_workout",
                title = "Power Walk",
                description = "Record 1 walking workout session today.",
                target = 1f,
                current = todaySessionsCount.toFloat(),
                type = ChallengeType.WORKOUT,
                isCompleted = todaySessionsCount >= 1,
                isClaimed = claimedSet.contains("challenge_workout")
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<Achievement>> = combine(
        dailyRecords,
        todaySteps,
        currentStreak,
        repository.plantLevel,
        repository.unlockedAchievements
    ) { records, steps, streak, plantLvl, unlockedIds ->
        val unlockedSet = unlockedIds.split(",").filter { it.isNotBlank() }.toSet()
        val cumulativeSteps = records.sumOf { it.steps } + steps
        val peakSteps = maxOf(records.maxOfOrNull { it.steps } ?: 0, steps)

        listOf(
            Achievement(
                id = "ach_first_steps",
                title = "First Steps",
                description = "Record your first step in StepUp.",
                targetValue = 1f,
                currentValue = cumulativeSteps.toFloat(),
                isUnlocked = cumulativeSteps >= 1,
                iconName = "directions_run"
            ),
            Achievement(
                id = "ach_streak_3",
                title = "Streak Starter",
                description = "Reach a 3-day walking streak.",
                targetValue = 3f,
                currentValue = streak.toFloat(),
                isUnlocked = streak >= 3,
                iconName = "local_fire_department"
            ),
            Achievement(
                id = "ach_streak_7",
                title = "Consistency King",
                description = "Achieve an active 7-day walking streak.",
                targetValue = 7f,
                currentValue = streak.toFloat(),
                isUnlocked = streak >= 7,
                iconName = "stars"
            ),
            Achievement(
                id = "ach_peak_10k",
                title = "Decathlon Walker",
                description = "Achieve 10,000 steps in a single day.",
                targetValue = 10000f,
                currentValue = peakSteps.toFloat(),
                isUnlocked = peakSteps >= 10000,
                iconName = "emoji_events"
            ),
            Achievement(
                id = "ach_plant_3",
                title = "Botanist Parent",
                description = "Grow your virtual plant companion to Level 3.",
                targetValue = 3f,
                currentValue = plantLvl.toFloat(),
                isUnlocked = plantLvl >= 3,
                iconName = "spa"
            ),
            Achievement(
                id = "ach_cumulative_50k",
                title = "Grand Voyager",
                description = "Log 50,000 steps in cumulative history.",
                targetValue = 50000f,
                currentValue = cumulativeSteps.toFloat(),
                isUnlocked = cumulativeSteps >= 50000,
                iconName = "hiking"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stepJourney: StateFlow<List<JourneyNode>> = combine(
        dailyRecords,
        todaySteps
    ) { records, steps ->
        val cumulativeSteps = records.sumOf { it.steps } + steps
        val nodes = listOf(
            JourneyNode("node_1", "Basecamp", "Your stepping journey begins here.", 0, "home"),
            JourneyNode("node_2", "Whispering Woods", "A quiet path beneath rustling maple leaves.", 10000, "nature_people"),
            JourneyNode("node_3", "Pebble Shore", "A serene lake path lined with smooth gray pebbles.", 25000, "waves"),
            JourneyNode("node_4", "Echo Canyon", "A magnificent gorge that amplifies your strides.", 50000, "terrain"),
            JourneyNode("node_5", "Summit Overlook", "Above the tree-line, looking down on the valleys.", 100000, "filter_hdr"),
            JourneyNode("node_6", "Nebula Station", "An interstellar walk amongst cosmic stars.", 200000, "brightness_low")
        )

        nodes.mapIndexed { index, node ->
            val isUnlocked = cumulativeSteps >= node.targetSteps
            val isCurrent = if (isUnlocked) {
                val nextNode = nodes.getOrNull(index + 1)
                nextNode == null || cumulativeSteps < nextNode.targetSteps
            } else false
            node.copy(isUnlocked = isUnlocked, isCurrent = isCurrent)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Active Walking Session States
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isSessionPaused = MutableStateFlow(false)
    val isSessionPaused: StateFlow<Boolean> = _isSessionPaused.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    private val _sessionDistanceKm = MutableStateFlow(0.0)
    val sessionDistanceKm: StateFlow<Double> = _sessionDistanceKm.asStateFlow()

    private val _sessionCalories = MutableStateFlow(0.0)
    val sessionCalories: StateFlow<Double> = _sessionCalories.asStateFlow()

    private var sessionJob: Job? = null

    init {
        loadOrCreateTodayRecord()

        // Seed some lovely initial notifications
        _notifications.value = listOf(
            InAppNotification(
                id = "init_notif_1",
                title = "Welcome to StepUp! 👟",
                message = "Ready to crush your goals? Start tracking steps and climb the landmarks journey!",
                timestamp = System.currentTimeMillis() - 60000,
                type = "GENERAL"
            ),
            InAppNotification(
                id = "init_notif_2",
                title = "Virtual Companion Ready 🌿",
                message = "Earn water drops for every 250 steps, then water your virtual plant in the 'Walk & Grow' tab!",
                timestamp = System.currentTimeMillis(),
                type = "PLANT"
            )
        )

        // Automatically monitor database records to update the streak engine
        viewModelScope.launch {
            combine(repository.allDailyRecords, repository.dailyStepGoal) { records, goal ->
                Pair(records, goal)
            }.collect { (records, goal) ->
                calculateAndStoreStreak(records, goal)
            }
        }

        // Monitor and trigger achievements notifications
        viewModelScope.launch {
            combine(achievements, repository.unlockedAchievements) { list, unlockedIds ->
                Pair(list, unlockedIds)
            }.collect { (list, unlockedIdsStr) ->
                val unlockedSet = unlockedIdsStr.split(",").filter { it.isNotBlank() }.toSet()
                val newlyUnlocked = list.filter { it.isUnlocked && !unlockedSet.contains(it.id) }
                
                if (newlyUnlocked.isNotEmpty()) {
                    val updatedList = unlockedSet.toMutableSet()
                    newlyUnlocked.forEach { ach ->
                        updatedList.add(ach.id)
                        postNotification(
                            title = "Achievement Unlocked! 🏆",
                            message = "You've unlocked the '${ach.title}' badge: ${ach.description}",
                            type = "ACHIEVEMENT"
                        )
                    }
                    repository.saveUnlockedAchievements(updatedList.joinToString(","))
                }
            }
        }
    }

    private fun calculateAndStoreStreak(records: List<DailyStepRecord>, goal: Int) {
        viewModelScope.launch {
            if (records.isEmpty()) {
                repository.updateStreak(0)
                return@launch
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val todayStr = sdf.format(cal.time)
            
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)

            val recordMap = records.associateBy { it.date }
            var streakCount = 0
            val checkCal = Calendar.getInstance()
            
            val todayRecord = recordMap[todayStr]
            val metToday = todayRecord != null && todayRecord.steps >= goal

            if (metToday) {
                streakCount++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                while (true) {
                    val dateKey = sdf.format(checkCal.time)
                    val rec = recordMap[dateKey]
                    if (rec != null && rec.steps >= rec.goal) {
                        streakCount++
                        checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            } else {
                val yesterdayRecord = recordMap[yesterdayStr]
                val metYesterday = yesterdayRecord != null && yesterdayRecord.steps >= yesterdayRecord.goal
                if (metYesterday) {
                    streakCount++
                    checkCal.add(Calendar.DAY_OF_YEAR, -2)
                    while (true) {
                        val dateKey = sdf.format(checkCal.time)
                        val rec = recordMap[dateKey]
                        if (rec != null && rec.steps >= rec.goal) {
                            streakCount++
                            checkCal.add(Calendar.DAY_OF_YEAR, -1)
                        } else {
                            break
                        }
                    }
                }
            }

            repository.updateStreak(streakCount)
        }
    }

    // Gamification Mechanics Functions
    fun postNotification(title: String, message: String, type: String) {
        val newNotification = InAppNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type
        )
        _notifications.value = listOf(newNotification) + _notifications.value
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun claimChallenge(challengeId: String) {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val claimedDate = repository.claimedChallengesDate.first()
            val claimedIdsStr = repository.claimedChallengesIds.first()

            val isSameDay = claimedDate == todayStr
            val claimedIdsList = if (isSameDay) {
                claimedIdsStr.split(",").filter { it.isNotBlank() }.toMutableList()
            } else {
                mutableListOf()
            }

            if (!claimedIdsList.contains(challengeId)) {
                claimedIdsList.add(challengeId)
                repository.saveClaimedChallenges(todayStr, claimedIdsList.joinToString(","))

                val currentEarned = repository.plantWaterEarned.first()
                val currentLevel = repository.plantLevel.first()
                val currentExp = repository.plantExp.first()
                val currentUsed = repository.plantWaterUsed.first()

                repository.savePlantState(
                    level = currentLevel,
                    exp = currentExp,
                    earned = currentEarned + 15,
                    used = currentUsed
                )

                val challengeTitle = when (challengeId) {
                    "challenge_steps_start" -> "Daily Warmup"
                    "challenge_steps_goal" -> "Peak Performer"
                    "challenge_calories" -> "Energy Burner"
                    "challenge_workout" -> "Power Walk"
                    else -> "Daily Challenge"
                }

                postNotification(
                    title = "Challenge Claimed! 💧",
                    message = "You received 15 drops of water for completing '$challengeTitle'!",
                    type = "CHALLENGE"
                )
            }
        }
    }

    fun waterPlant(drops: Int) {
        viewModelScope.launch {
            val level = repository.plantLevel.first()
            val exp = repository.plantExp.first()
            val earned = repository.plantWaterEarned.first()
            val used = repository.plantWaterUsed.first()

            val availableDrops = earned - used
            if (availableDrops >= drops) {
                val newUsed = used + drops
                var newExp = exp + (drops * 15)
                var newLevel = level
                var maxExp = newLevel * 100
                var leveledUp = false

                while (newExp >= maxExp) {
                    newExp -= maxExp
                    newLevel++
                    maxExp = newLevel * 100
                    leveledUp = true
                }

                repository.savePlantState(
                    level = newLevel,
                    exp = newExp,
                    earned = earned,
                    used = newUsed
                )

                postNotification(
                    title = "Plant Watered! 🌿",
                    message = "Gave $drops drop(s) of water. Gained ${drops * 15} XP!",
                    type = "PLANT"
                )

                if (leveledUp) {
                    val plantName = when (newLevel) {
                        2 -> "Little Seedling"
                        3 -> "Budding Clover"
                        4 -> "Flowering Fern"
                        5 -> "Golden Bloom"
                        else -> "Mighty Oak Tree"
                    }
                    postNotification(
                        title = "Companion Leveled Up! 🎉🌱",
                        message = "Congratulations! Your plant grew into a $plantName (Level $newLevel)!",
                        type = "PLANT"
                    )
                }
            }
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(time1)) == sdf.format(Date(time2))
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadOrCreateTodayRecord() {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val record = repository.getTodayRecord(dateStr)
            if (record != null) {
                _todaySteps.value = record.steps
                _todayCalories.value = record.calories
                _todayDistanceKm.value = record.distanceKm
                _todayTimeMinutes.value = record.walkingTimeMinutes
            } else {
                val newRecord = DailyStepRecord(
                    date = dateStr,
                    steps = 0,
                    goal = dailyStepGoal.value,
                    calories = 0,
                    distanceKm = 0.0,
                    walkingTimeMinutes = 0
                )
                repository.insertDailyRecord(newRecord)
                _todaySteps.value = 0
                _todayCalories.value = 0
                _todayDistanceKm.value = 0.0
                _todayTimeMinutes.value = 0
            }
        }
    }

    private fun saveTodayRecord() {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val record = DailyStepRecord(
                date = dateStr,
                steps = _todaySteps.value,
                goal = dailyStepGoal.value,
                calories = _todayCalories.value,
                distanceKm = _todayDistanceKm.value,
                walkingTimeMinutes = _todayTimeMinutes.value
            )
            repository.insertDailyRecord(record)
        }
    }

    // Interactive Step Increment Simulation
    fun simulateSteps(count: Int) {
        viewModelScope.launch {
            val stepsOld = _todaySteps.value
            val stepsNew = (stepsOld + count).coerceAtLeast(0)
            _todaySteps.value = stepsNew
            
            val strideCm = userStrideLength.value
            val weight = userWeight.value
            
            // Real-time Formulas:
            // Distance (km) = (Steps * Stride Length cm) / 100,000
            val distanceKm = (stepsNew * strideCm.toDouble()) / 100000.0
            // Calories (kcal) = Steps * 0.04 * (Weight kg / 70.0)
            val calories = (stepsNew * 0.04 * (weight / 70.0)).toInt()
            // Active Time (min) = (Steps * 0.01).toInt()
            val timeMinutes = if (stepsNew > 0) (stepsNew * 0.01).toInt().coerceAtLeast(1) else 0

            _todayDistanceKm.value = distanceKm
            _todayCalories.value = calories
            _todayTimeMinutes.value = timeMinutes

            saveTodayRecord()

            val oldDrops = stepsOld / 250
            val newDrops = stepsNew / 250
            if (newDrops > oldDrops) {
                val diff = newDrops - oldDrops
                val level = repository.plantLevel.first()
                val exp = repository.plantExp.first()
                val earned = repository.plantWaterEarned.first()
                val used = repository.plantWaterUsed.first()
                
                repository.savePlantState(
                    level = level,
                    exp = exp,
                    earned = earned + diff,
                    used = used
                )
                
                postNotification(
                    title = "Water Drops Earned! 💧",
                    message = "You earned $diff water drop(s) from walking! Nourish your plant companion.",
                    type = "PLANT"
                )
            }
        }
    }

    fun resetTodaySteps() {
        viewModelScope.launch {
            _todaySteps.value = 0
            _todayDistanceKm.value = 0.0
            _todayCalories.value = 0
            _todayTimeMinutes.value = 0
            saveTodayRecord()
        }
    }

    // Physical Sensor Step Handling - Single Source of Truth
    fun recordSensorSteps(count: Int) {
        if (count <= 0) return
        viewModelScope.launch {
            val stepsOld = _todaySteps.value
            val stepsNew = stepsOld + count
            _todaySteps.value = stepsNew
            
            val strideCm = userStrideLength.value
            val weight = userWeight.value
            
            // Real-time Formulas:
            // Distance (km) = (Steps * Stride Length cm) / 100,000
            val distanceKm = (stepsNew * strideCm.toDouble()) / 100000.0
            // Calories (kcal) = Steps * 0.04 * (Weight kg / 70.0)
            val calories = (stepsNew * 0.04 * (weight / 70.0)).toInt()
            // Active Time (min)
            val timeMinutes = if (stepsNew > 0) (stepsNew * 0.01).toInt().coerceAtLeast(1) else 0

            _todayDistanceKm.value = distanceKm
            _todayCalories.value = calories
            _todayTimeMinutes.value = timeMinutes

            saveTodayRecord()

            // If a walking session is active and unpaused, update session stride metrics
            if (_isSessionActive.value && !_isSessionPaused.value) {
                val newSessionSteps = _sessionSteps.value + count
                _sessionSteps.value = newSessionSteps
                _sessionDistanceKm.value = (newSessionSteps * strideCm.toDouble()) / 100000.0
                _sessionCalories.value = newSessionSteps * 0.04 * (weight / 70.0)
            }

            val oldDrops = stepsOld / 250
            val newDrops = stepsNew / 250
            if (newDrops > oldDrops) {
                val diff = newDrops - oldDrops
                val level = repository.plantLevel.first()
                val exp = repository.plantExp.first()
                val earned = repository.plantWaterEarned.first()
                val used = repository.plantWaterUsed.first()
                
                repository.savePlantState(
                    level = level,
                    exp = exp,
                    earned = earned + diff,
                    used = used
                )
                
                postNotification(
                    title = "Water Drops Earned! 💧",
                    message = "Your strides earned you $diff water drop(s). Check 'Walk & Grow'!",
                    type = "PLANT"
                )
            }
        }
    }

    fun hasActivityRecognitionPermission(): Boolean = sensorTracker.hasActivityRecognitionPermission()

    fun startSensorTracking() {
        sensorTracker.startTracking()
    }

    fun stopSensorTracking() {
        sensorTracker.stopTracking()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorTracking()
    }

    // Active Walking Session Controls
    fun startWalkingSession() {
        _isSessionActive.value = true
        _isSessionPaused.value = false
        _sessionDurationSeconds.value = 0L
        _sessionSteps.value = 0
        _sessionDistanceKm.value = 0.0
        _sessionCalories.value = 0.0

        startSessionTimer()
    }

    private fun startSessionTimer() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            while (_isSessionActive.value && !_isSessionPaused.value) {
                delay(1000)
                _sessionDurationSeconds.value += 1L
            }
        }
    }

    fun pauseWalkingSession() {
        _isSessionPaused.value = true
        sessionJob?.cancel()
    }

    fun resumeWalkingSession() {
        _isSessionPaused.value = false
        startSessionTimer()
    }

    fun stopAndSaveWalkingSession() {
        sessionJob?.cancel()
        val duration = _sessionDurationSeconds.value
        val steps = _sessionSteps.value
        val distance = _sessionDistanceKm.value
        val calories = _sessionCalories.value

        _isSessionActive.value = false
        _isSessionPaused.value = false

        viewModelScope.launch {
            repository.insertWalkingSession(
                WalkingSession(
                    startTime = System.currentTimeMillis(),
                    durationSeconds = duration,
                    steps = steps,
                    calories = calories,
                    distanceKm = distance
                )
            )
        }
    }

    fun cancelWalkingSession() {
        sessionJob?.cancel()
        _isSessionActive.value = false
        _isSessionPaused.value = false
        _sessionDurationSeconds.value = 0L
        _sessionSteps.value = 0
        _sessionDistanceKm.value = 0.0
        _sessionCalories.value = 0.0
    }

    // Settings Profile Operations - Dynamic Recalculation
    fun saveProfile(name: String, goal: Int, weight: Float, height: Float, strideLength: Float = height * 0.414f) {
        viewModelScope.launch {
            repository.saveProfile(name, goal, weight, height, strideLength)
            // Immediately dynamically recalculate distance and calories for today's record
            val currentSteps = _todaySteps.value
            val dist = (currentSteps * strideLength.toDouble()) / 100000.0
            val cal = (currentSteps * 0.04 * (weight / 70.0)).toInt()
            val time = if (currentSteps > 0) (currentSteps * 0.01).toInt().coerceAtLeast(1) else 0

            _todayDistanceKm.value = dist
            _todayCalories.value = cal
            _todayTimeMinutes.value = time

            saveTodayRecord()
        }
    }

    fun setCustomGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.setCustomGeminiApiKey(apiKey)
        }
    }

    private fun getEffectiveApiKey(): String {
        val custom = customGeminiApiKey.value.trim()
        if (custom.isNotBlank()) return custom
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    // AI Fitness Coach Engine (Generative Gemini with Multi-Turn Memory & Streaming)
    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userPrompt = prompt.trim()
        val userMsg = ChatMessage(text = userPrompt, isUser = true)
        val currentHistory = _chatMessages.value
        _chatMessages.value = currentHistory + userMsg
        _isAiTyping.value = true

        val apiKey = getEffectiveApiKey()
        val systemPrompt = buildCoachSystemPrompt()

        // Build multi-turn conversational history pairs (text, isUser)
        val conversationTurns = currentHistory.map { Pair(it.text, it.isUser) }

        viewModelScope.launch {
            if (apiKey.isBlank()) {
                // Intelligent Offline Fallback
                delay(800)
                val fallbackText = generateContextAwareCoachResponse(userPrompt)
                val aiMsg = ChatMessage(
                    text = fallbackText,
                    isUser = false,
                    isOfflineFallback = true
                )
                _chatMessages.value = _chatMessages.value + aiMsg
                _isAiTyping.value = false
            } else {
                // Generative Gemini API Call
                val aiPlaceholderId = UUID.randomUUID().toString()
                var streamingMsgAdded = false

                val result = geminiService.generateResponse(
                    apiKey = apiKey,
                    conversationHistory = conversationTurns,
                    userPrompt = userPrompt,
                    systemInstructionText = systemPrompt,
                    onChunkReceived = { chunkText ->
                        _isAiTyping.value = false
                        if (!streamingMsgAdded) {
                            streamingMsgAdded = true
                            val initialAiMsg = ChatMessage(
                                id = aiPlaceholderId,
                                text = chunkText,
                                isUser = false,
                                isStreaming = true
                            )
                            _chatMessages.value = _chatMessages.value + initialAiMsg
                        } else {
                            _chatMessages.value = _chatMessages.value.map { msg ->
                                if (msg.id == aiPlaceholderId) {
                                    msg.copy(text = chunkText, isStreaming = true)
                                } else msg
                            }
                        }
                    }
                )

                _isAiTyping.value = false

                result.onSuccess { finalText ->
                    if (streamingMsgAdded) {
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == aiPlaceholderId) {
                                msg.copy(text = finalText, isStreaming = false, isOfflineFallback = false)
                            } else msg
                        }
                    } else {
                        val aiMsg = ChatMessage(
                            id = aiPlaceholderId,
                            text = finalText,
                            isUser = false,
                            isStreaming = false,
                            isOfflineFallback = false
                        )
                        _chatMessages.value = _chatMessages.value + aiMsg
                    }
                }.onFailure { _ ->
                    // On API error or quota, gracefully fallback to smart local rules
                    val fallbackText = generateContextAwareCoachResponse(userPrompt)
                    if (streamingMsgAdded) {
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == aiPlaceholderId) {
                                msg.copy(text = fallbackText, isStreaming = false, isOfflineFallback = true)
                            } else msg
                        }
                    } else {
                        val aiMsg = ChatMessage(
                            id = aiPlaceholderId,
                            text = fallbackText,
                            isUser = false,
                            isStreaming = false,
                            isOfflineFallback = true
                        )
                        _chatMessages.value = _chatMessages.value + aiMsg
                    }
                }
            }
        }
    }

    private fun buildCoachSystemPrompt(): String {
        val steps = _todaySteps.value
        val goal = dailyStepGoal.value
        val calories = _todayCalories.value
        val distanceKm = _todayDistanceKm.value
        val timeMinutes = _todayTimeMinutes.value
        val weight = userWeight.value
        val stride = userStrideLength.value
        val streak = currentStreak.value
        val plantLvl = plantState.value.level
        val plantXp = plantState.value.currentExp
        val remainingSteps = (goal - steps).coerceAtLeast(0)
        val progressPct = if (goal > 0) ((steps.toFloat() / goal) * 100).toInt() else 0
        val isMetric = useMetric.value

        return """
            You are StepUp AI, an intelligent, empathetic, highly motivating, and knowledgeable fitness & walking coach.
            You have access to the user's real-time step and wellness metrics:
            - Steps Today: $steps / $goal ($progressPct% completed)
            - Remaining Steps to Goal: $remainingSteps
            - Active Calories Burned Today: $calories kcal
            - Distance Covered: ${String.format("%.2f", distanceKm)} ${if (isMetric) "km" else "miles"}
            - Active Walking Time: $timeMinutes mins
            - Current Daily Streak: $streak days
            - User Profile: Weight ${weight.toInt()} kg, Stride Length ${stride.toInt()} cm
            - Virtual Plant Garden: Level $plantLvl ($plantXp XP)

            Coach Personality & Style:
            - Provide clear, actionable, friendly, and empowering health, walking, diet, workout, hydration, and motivation advice.
            - Answer questions accurately and concisely. Keep responses structured with clean Markdown (bullet points, bold key stats, numbered lists).
            - Whenever relevant, reference the user's live numbers (e.g. remaining steps, calories burned, streak).
        """.trimIndent()
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Hello! 👟 I'm your StepUp AI Fitness Coach powered by Gemini. I can help analyze your step pace, calories burned, remaining distance, nutrition ideas, or provide personalized walking workout tips. What's on your mind today?",
                isUser = false
            )
        )
    }

    private fun generateContextAwareCoachResponse(userQuery: String): String {
        val steps = _todaySteps.value
        val goal = dailyStepGoal.value
        val calories = _todayCalories.value
        val distanceKm = _todayDistanceKm.value
        val timeMinutes = _todayTimeMinutes.value
        val weight = userWeight.value
        val stride = userStrideLength.value
        val streak = currentStreak.value
        val remainingSteps = (goal - steps).coerceAtLeast(0)
        val progressPercent = if (goal > 0) ((steps.toFloat() / goal) * 100).toInt() else 0

        val q = userQuery.lowercase()

        return when {
            q.contains("steps left") || q.contains("remaining") || q.contains("how many steps") || q.contains("finish") -> {
                if (remainingSteps == 0) {
                    "🎉 **Incredible achievement!** You've crushed your daily goal of $goal steps with **${String.format("%,d", steps)} steps** completed today ($progressPercent%). Any additional steps are pure bonus fitness gains!"
                } else {
                    val remainingDistanceKm = (remainingSteps * stride) / 100000.0
                    val remainingMinutes = (remainingSteps * 0.01).toInt().coerceAtLeast(1)
                    "You have **${String.format("%,d", remainingSteps)} steps** left to hit your daily goal of **${String.format("%,d", goal)} steps** ($progressPercent% completed).\n\n• **Estimated Distance**: ${String.format("%.2f", remainingDistanceKm)} km\n• **Estimated Walk Time**: ~$remainingMinutes mins brisk stroll\n• **Tip**: A quick 15-minute evening walk will get you across the finish line! 🚶"
                }
            }
            q.contains("snack") || q.contains("diet") || q.contains("food") || q.contains("nutrition") || q.contains("eat") -> {
                "🥗 **Healthy Fuel & Snack Ideas for Walkers**:\n\n* **Pre-Walk**: Half a banana with a tablespoon of peanut butter, or an apple with a handful of almonds.\n* **Post-Walk Recovery**: Greek yogurt with blueberries and chia seeds (rich in protein & antioxidants).\n* **Hydration**: Drink 250ml water infused with lemon or cucumber.\n* **Energy Boost**: 1-2 Medjool dates with walnuts for sustained glycogen replenishment."
            }
            q.contains("pacing") || q.contains("pace") || q.contains("speed") -> {
                "⏱️ **Optimal Walking Pacing Guide**:\n\n* **Easy Recovery Stroll**: 80–100 steps/min (good for warming up or active rest).\n* **Brisk Aerobic Walk**: 100–120 steps/min (elevates heart rate and burns optimal fat).\n* **Power Walking**: 120–140 steps/min (swings arms at 90°, burns up to 30% more calories).\n* **Today's Goal**: Maintain a steady ~110 steps/min cadence to finish your remaining $remainingSteps steps in under 20 mins."
            }
            q.contains("calorie") || q.contains("burn") || q.contains("kcal") || q.contains("fat") -> {
                val calPerStep = 0.04 * (weight / 70.0)
                "🔥 **Calorie & Energy Burn Breakdown**:\n\n• **Total Burned Today**: **$calories kcal** across **${String.format("%,d", steps)} steps**\n• **Efficiency Rate**: ~${String.format("%.3f", calPerStep)} kcal per step (adjusted for your weight of ${weight.toInt()} kg)\n• **Power Tip**: Power walking uphill or taking the stairs doubles calorie expenditure and strengthens calves & glutes."
            }
            q.contains("tip") || q.contains("advice") || q.contains("walk") || q.contains("posture") -> {
                "💡 **Pro Walking Form & Health Tips**:\n\n1. **Posture**: Keep head up, eyes 15 feet ahead, and shoulders relaxed down and back.\n2. **Footstrike**: Roll smoothly from heel to toe for maximum joint protection.\n3. **Arm Motion**: Keep elbows bent at 90° and pump straight forward, not across your chest.\n4. **Hydration**: Drink water regularly—especially after reaching 5,000 steps."
            }
            q.contains("stretch") || q.contains("recovery") || q.contains("sore") -> {
                "🧘 **Post-Walk Stretch & Recovery Routine**:\n\n1. **Calf Stretch**: Place hands on a wall, step one leg back, press heel flat for 30s per leg.\n2. **Hamstring Stretch**: Prop heel on a step, hinge hips back gently with a straight spine.\n3. **Quad Stretch**: Hold one ankle behind your glutes, keeping knees aligned.\n4. **Hydrate & Rest**: Elevate feet for 5 minutes to reduce leg fatigue."
            }
            q.contains("on track") || q.contains("status") || q.contains("progress") -> {
                if (progressPercent >= 100) {
                    "🌟 **Outstanding Performance!** You are at **$progressPercent%** of your goal with **${String.format("%,d", steps)} steps** logged. Your **$streak-day streak** is on fire! 🔥"
                } else if (progressPercent >= 50) {
                    "👍 **Strong Momentum!** You've completed **$progressPercent%** (${String.format("%,d", steps)} / ${String.format("%,d", goal)} steps). A 15-minute afternoon walk will comfortably close the gap."
                } else {
                    "👟 **Let's Get Moving!** You're currently at **$progressPercent%** (${String.format("%,d", steps)} steps). You need **${String.format("%,d", remainingSteps)} more steps** today. Break it into two 10-minute walk intervals!"
                }
            }
            q.contains("garden") || q.contains("plant") || q.contains("water") -> {
                val dropsEarned = steps / 250
                val nextDropIn = 250 - (steps % 250)
                "🌱 **Virtual Garden Companion**:\n\n• **Water Drops Earned**: **$dropsEarned drops** (1 drop every 250 steps)\n• **Next Drop Available In**: **$nextDropIn steps**\n• **Companion Level**: Level ${plantState.value.level}\n• Visit the Garden tab to nourish your plant and unlock new leaf stages!"
            }
            q.contains("streak") || q.contains("record") -> {
                "🔥 **Streak & Endurance Status**:\n\n• **Active Goal Streak**: **$streak Days** in a row\n• **Total Walked Today**: **${String.format("%.2f", distanceKm)} km** in **$timeMinutes mins**\n• Keep your daily rhythm alive to climb up the achievement ladder!"
            }
            else -> {
                "👟 **StepUp Fitness Coach Insights**:\n\nYou've recorded **${String.format("%,d", steps)} steps** today (**${String.format("%.2f", distanceKm)} km**, **$calories kcal** burned) towards your **${String.format("%,d", goal)} step goal**.\n\nAsk me anything about customized pacing, cardio fat-burning zones, stretching routines, or meal ideas to fuel your steps!"
            }
        }
    }

    fun toggleUnits() {
        viewModelScope.launch {
            val current = useMetric.value
            repository.setUseMetric(!current)
        }
    }

    fun toggleTheme(dark: Boolean?) {
        viewModelScope.launch {
            repository.setDarkTheme(dark)
        }
    }

    fun login() {
        viewModelScope.launch {
            repository.setLoggedIn(true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.setLoggedIn(false)
            repository.clearAllData()
            loadOrCreateTodayRecord()
        }
    }
}
