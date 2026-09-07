package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyStepRecord
import com.example.ui.theme.LightPrimary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsView(
    dailyRecords: List<DailyStepRecord>,
    useMetric: Boolean,
    defaultGoal: Int
) {
    var selectedRangeTab by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    val rangeTabs = listOf("Weekly (7d)", "Monthly (30d)")

    // Pre-calculate chronological data
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNameSdf = SimpleDateFormat("EEE", Locale.getDefault())
    val monthDaySdf = SimpleDateFormat("MMM d", Locale.getDefault())

    // Last 7 days items
    val weeklyItems = remember(dailyRecords, defaultGoal) {
        val cal = Calendar.getInstance()
        (0..6).map { offset ->
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.DAY_OF_YEAR, -offset)
            val dateKey = sdf.format(checkCal.time)
            val dayName = dayNameSdf.format(checkCal.time)
            val record = dailyRecords.firstOrNull { it.date == dateKey }
            val steps = record?.steps ?: 0
            val goal = record?.goal ?: defaultGoal
            ChartItem(
                dateStr = dateKey,
                label = dayName,
                steps = steps,
                goal = goal,
                calories = record?.calories ?: 0,
                distanceKm = record?.distanceKm ?: 0.0,
                isMet = steps >= goal
            )
        }.reversed()
    }

    // Last 30 days items
    val monthlyItems = remember(dailyRecords, defaultGoal) {
        (0..29).map { offset ->
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.DAY_OF_YEAR, -offset)
            val dateKey = sdf.format(checkCal.time)
            val dayName = monthDaySdf.format(checkCal.time)
            val record = dailyRecords.firstOrNull { it.date == dateKey }
            val steps = record?.steps ?: 0
            val goal = record?.goal ?: defaultGoal
            ChartItem(
                dateStr = dateKey,
                label = dayName,
                steps = steps,
                goal = goal,
                calories = record?.calories ?: 0,
                distanceKm = record?.distanceKm ?: 0.0,
                isMet = steps >= goal
            )
        }.reversed()
    }

    val currentItems = if (selectedRangeTab == 0) weeklyItems else monthlyItems

    // Calculate Analytics Statistics
    val stats = remember(currentItems) {
        val totalSteps = currentItems.sumOf { it.steps }
        val avgSteps = if (currentItems.isNotEmpty()) totalSteps / currentItems.size else 0
        val peakItem = currentItems.maxByOrNull { it.steps }
        val goalsMet = currentItems.count { it.isMet }
        val completionPercent = if (currentItems.isNotEmpty()) (goalsMet.toFloat() / currentItems.size.toFloat() * 100).toInt() else 0
        val totalCalories = currentItems.sumOf { it.calories }
        val totalDistance = currentItems.sumOf { it.distanceKm }

        AnalyticsStats(
            totalSteps = totalSteps,
            avgSteps = avgSteps,
            peakSteps = peakItem?.steps ?: 0,
            peakDateLabel = peakItem?.label ?: "-",
            completionPercent = completionPercent,
            goalsMetCount = goalsMet,
            totalDays = currentItems.size,
            totalCalories = totalCalories,
            totalDistance = totalDistance
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Toggle tabs for Weekly vs Monthly
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rangeTabs.forEachIndexed { index, title ->
                    val selected = selectedRangeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedRangeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Chart Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = if (selectedRangeTab == 0) "Weekly Progress" else "Monthly Trends",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap any bar to see specific steps and records",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Render Chart based on tab
                if (selectedRangeTab == 0) {
                    WeeklyBarChart(weeklyItems)
                } else {
                    MonthlyScrollableChart(monthlyItems)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aggregate Weekly Comparison if Monthly is selected
        if (selectedRangeTab == 1) {
            WeeklyAggregateCard(monthlyItems)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary Insights Grid
        Text(
            text = "Analytical Insights",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Row 1: Average Daily & Goal Completion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsInsightItemCard(
                    title = "Daily Average",
                    value = String.format("%,d", stats.avgSteps),
                    subtitle = "steps / day",
                    icon = Icons.Filled.Timeline,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = if (MaterialTheme.colorScheme.primary == LightPrimary) Color(0xFFF2F1E9) else Color(0xFF2A2B23)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsInsightItemCard(
                    title = "Goal Success",
                    value = "${stats.completionPercent}%",
                    subtitle = "${stats.goalsMetCount} of ${stats.totalDays} days met",
                    icon = Icons.Filled.Stars,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    iconBgColor = if (MaterialTheme.colorScheme.primary == LightPrimary) Color(0xFFFBEBE8) else Color(0xFF3B2622)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Peak Steps & Cumulative Distance
        val df = DecimalFormat("#.##")
        val distanceText = if (useMetric) {
            "${df.format(stats.totalDistance)} km"
        } else {
            "${df.format(stats.totalDistance * 0.621371)} mi"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsInsightItemCard(
                    title = "Personal Peak",
                    value = String.format("%,d", stats.peakSteps),
                    subtitle = "on ${stats.peakDateLabel}",
                    icon = Icons.Filled.EmojiEvents,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    iconBgColor = if (MaterialTheme.colorScheme.primary == LightPrimary) Color(0xFFEBF6F8) else Color(0xFF1E2D30)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsInsightItemCard(
                    title = "Total Distance",
                    value = distanceText,
                    subtitle = "${String.format("%,d", stats.totalCalories)} kcal burned",
                    icon = Icons.Filled.Hiking,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = if (MaterialTheme.colorScheme.primary == LightPrimary) Color(0xFFEDF5E7) else Color(0xFF232D1F)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WeeklyBarChart(items: List<ChartItem>) {
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val maxSteps = remember(items) { items.maxOfOrNull { it.steps }?.coerceAtLeast(1000) ?: 10000 }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Detailed inspect banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (selectedItemIndex in items.indices) {
                val item = items[selectedItemIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.label}: ${String.format("%,d", item.steps)} steps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.isMet) "Goal met! 🎉" else "Goal: ${String.format("%,d", item.goal)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (item.isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Select a bar to view detailed steps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Main Bar Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEachIndexed { index, item ->
                val barHeightFraction = item.steps.toFloat() / maxSteps.toFloat()
                val animatedHeightFraction by animateFloatAsState(
                    targetValue = barHeightFraction.coerceIn(0.05f, 1f),
                    animationSpec = tween(durationMillis = 800)
                )
                val isSelected = selectedItemIndex == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Checkmark if Goal Met
                    if (item.isMet) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Met",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(bottom = 2.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight(animatedHeightFraction * 0.82f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else if (item.isMet) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                                }
                            )
                            .clickable { selectedItemIndex = index }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Label (e.g., Mon)
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyScrollableChart(items: List<ChartItem>) {
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val maxSteps = remember(items) { items.maxOfOrNull { it.steps }?.coerceAtLeast(1000) ?: 10000 }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Detailed Inspect banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (selectedItemIndex in items.indices) {
                val item = items[selectedItemIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.label}: ${String.format("%,d", item.steps)} steps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.isMet) "Goal met! 🎉" else "Goal: ${String.format("%,d", item.goal)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (item.isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Swipe left/right to view 30 days of data",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Horizontal Scrollable Bar Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEachIndexed { index, item ->
                val barHeightFraction = item.steps.toFloat() / maxSteps.toFloat()
                val isSelected = selectedItemIndex == index

                Column(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Miniscule Dot indicating Goal met
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isMet) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(barHeightFraction * 0.85f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else if (item.isMet) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                }
                            )
                            .clickable { selectedItemIndex = index }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Clean small numbered labels for days
                    Text(
                        text = item.dateStr.split("-").lastOrNull() ?: "",
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyAggregateCard(items: List<ChartItem>) {
    // Group the 30 days into 4 weeks of progress aggregates
    val weeklyAggregates = remember(items) {
        val reversedChunks = items.chunked(7)
        reversedChunks.mapIndexed { index, chunk ->
            val totalSteps = chunk.sumOf { it.steps }
            val avgSteps = if (chunk.isNotEmpty()) totalSteps / chunk.size else 0
            val metDays = chunk.count { it.isMet }
            WeeklyAggregate(
                weekLabel = "Week ${index + 1}",
                totalSteps = totalSteps,
                avgSteps = avgSteps,
                metDays = metDays
            )
        }
    }

    val maxWeeklySteps = remember(weeklyAggregates) {
        weeklyAggregates.maxOfOrNull { it.totalSteps }?.coerceAtLeast(1000) ?: 50000
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Weekly Progress Breakdown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            weeklyAggregates.forEach { agg ->
                val progressFraction = agg.totalSteps.toFloat() / maxWeeklySteps.toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = agg.weekLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${String.format("%,d", agg.totalSteps)} steps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${agg.metDays}/7 days met",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Custom Progress Bar
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsInsightItemCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// Helpers Data Models
data class ChartItem(
    val dateStr: String,
    val label: String,
    val steps: Int,
    val goal: Int,
    val calories: Int,
    val distanceKm: Double,
    val isMet: Boolean
)

data class AnalyticsStats(
    val totalSteps: Int,
    val avgSteps: Int,
    val peakSteps: Int,
    val peakDateLabel: String,
    val completionPercent: Int,
    val goalsMetCount: Int,
    val totalDays: Int,
    val totalCalories: Int,
    val totalDistance: Double
)

data class WeeklyAggregate(
    val weekLabel: String,
    val totalSteps: Int,
    val avgSteps: Int,
    val metDays: Int
)
