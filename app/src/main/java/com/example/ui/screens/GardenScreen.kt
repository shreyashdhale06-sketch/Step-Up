package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StepViewModel

@Composable
fun GardenScreen(
    viewModel: StepViewModel
) {
    val plantState by viewModel.plantState.collectAsState()
    val todaySteps by viewModel.todaySteps.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val allTimeHighestSteps by viewModel.allTimeHighestSteps.collectAsState()

    val scrollState = rememberScrollState()

    val progressFraction = if (dailyStepGoal > 0) todaySteps.toFloat() / dailyStepGoal.toFloat() else 0f
    val progressPercentage = (progressFraction * 100).toInt()

    // Dynamic stage based on daily goal progress
    val currentStageIndex = when {
        progressPercentage >= 100 -> 3 // Tree
        progressPercentage >= 70 -> 2  // Plant
        progressPercentage >= 25 -> 1  // Sapling
        else -> 0                      // Seed
    }

    val (stageName, stageEmoji, stageDesc) = when (currentStageIndex) {
        3 -> Triple("Blooming Tree", "🌸🌳", "Goal Reached (100%+) • Fully Blossomed")
        2 -> Triple("Budding Plant", "🪴", "Almost There (70-99%) • Buds Opening")
        1 -> Triple("Growing Sapling", "🌿", "On Track (25-69%) • Strong Roots")
        else -> Triple("Sprouting Seed", "🌱", "Early Phase (0-24%) • Germinating")
    }

    val availableDrops = (plantState.totalWaterEarned - plantState.totalWaterUsed).coerceAtLeast(0)
    var selectedDropsToGive by remember { mutableStateOf(1) }

    LaunchedEffect(availableDrops) {
        if (selectedDropsToGive > availableDrops && availableDrops > 0) {
            selectedDropsToGive = availableDrops
        } else if (availableDrops == 0) {
            selectedDropsToGive = 1
        }
    }

    val expProgress = if (plantState.maxExp > 0) {
        (plantState.currentExp.toFloat() / plantState.maxExp.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedExp by animateFloatAsState(
        targetValue = expProgress,
        animationSpec = tween(durationMillis = 600),
        label = "expProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Garden",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Walk to nourish your dynamic botanical companion",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Level ${plantState.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Streak & Highest Record Stat Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFBEBE8), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ACTIVE STREAK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "$currentStreak Days 🔥",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Highest Daily Record Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFF8E1), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Record",
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HIGHEST RECORD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (allTimeHighestSteps > 0) "${String.format("%,d", allTimeHighestSteps)}" else "0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Growth Stage Card (Canvas & Stage indicator)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stage Header Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stageEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stageName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stageDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$progressPercentage%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4-Stage Visual Stepper (Seed -> Sapling -> Plant -> Tree)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stagesList = listOf(
                        "Seed" to "🌱",
                        "Sapling" to "🌿",
                        "Plant" to "🪴",
                        "Tree" to "🌸🌳"
                    )

                    stagesList.forEachIndexed { index, (name, icon) ->
                        val isUnlocked = index <= currentStageIndex
                        val isCurrent = index == currentStageIndex

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else if (isUnlocked) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else if (isUnlocked) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Botanical Companion Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlantCompanionCanvas(level = plantState.level.coerceAtLeast(currentStageIndex + 1))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Experience (XP) Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Growth Experience",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${plantState.currentExp} / ${plantState.maxExp} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedExp },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Feed water drops (+15 XP each) to level up your garden.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Water Drops Inventory & Nourish Controller Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Water Drops",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$availableDrops",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "water drops available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Earned automatically while walking: 250 steps = 1 water drop 💧",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableDrops > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledIconButton(
                            onClick = { if (selectedDropsToGive > 1) selectedDropsToGive-- },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Remove, "Decrease", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "$selectedDropsToGive",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FilledIconButton(
                            onClick = { if (selectedDropsToGive < availableDrops) selectedDropsToGive++ },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Increase", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { selectedDropsToGive = availableDrops },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Max", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.waterPlant(selectedDropsToGive) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("water_plant_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WaterDrop, "Water", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Feed $selectedDropsToGive Drops (+${selectedDropsToGive * 15} XP)", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Take a walk to earn more water drops for your garden companion! 🌿",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCompanionCanvas(level: Int) {
    Canvas(
        modifier = Modifier
            .size(220.dp)
            .padding(12.dp)
    ) {
        val width = size.width
        val height = size.height

        val potY = height * 0.75f
        val centerX = width / 2.0f

        val potWidthBottom = 55f
        val potWidthTop = 75f
        val potHeight = 45f

        val potPath = Path().apply {
            moveTo(centerX - potWidthTop, potY)
            lineTo(centerX + potWidthTop, potY)
            lineTo(centerX + potWidthBottom, potY + potHeight)
            lineTo(centerX - potWidthBottom, potY + potHeight)
            close()
        }

        // Draw soil inside pot
        drawOval(
            color = Color(0xFF5D4037),
            topLeft = Offset(centerX - potWidthTop + 4f, potY - 10f),
            size = Size(potWidthTop * 2f - 8f, 20f)
        )

        // Draw Plant structures based on level
        when (level) {
            1 -> {
                // SPROUT / SEED
                drawArc(
                    color = Color(0xFF4CAF50),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(centerX - 25f, potY - 30f),
                    size = Size(50f, 50f),
                    style = Stroke(width = 8f)
                )
                drawOval(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(centerX - 10f, potY - 38f),
                    size = Size(16f, 10f)
                )
            }
            2 -> {
                // SEEDLING / SAPLING
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = Offset(centerX, potY - 8f),
                    end = Offset(centerX, potY - 70f),
                    strokeWidth = 10f
                )
                drawOval(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(centerX - 30f, potY - 55f),
                    size = Size(25f, 15f)
                )
                drawOval(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(centerX + 5f, potY - 65f),
                    size = Size(25f, 15f)
                )
            }
            3 -> {
                // BUDDING PLANT
                drawLine(
                    color = Color(0xFF388E3C),
                    start = Offset(centerX, potY - 8f),
                    end = Offset(centerX, potY - 100f),
                    strokeWidth = 12f
                )
                drawLine(
                    color = Color(0xFF388E3C),
                    start = Offset(centerX, potY - 50f),
                    end = Offset(centerX - 40f, potY - 80f),
                    strokeWidth = 10f
                )
                drawOval(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(centerX - 55f, potY - 95f),
                    size = Size(30f, 18f)
                )
                drawOval(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(centerX + 10f, potY - 75f),
                    size = Size(28f, 16f)
                )
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 12f,
                    center = Offset(centerX, potY - 106f)
                )
            }
            4 -> {
                // FLOWERING BUSH
                drawLine(
                    color = Color(0xFF2E7D32),
                    start = Offset(centerX, potY - 8f),
                    end = Offset(centerX, potY - 120f),
                    strokeWidth = 12f
                )
                drawLine(
                    color = Color(0xFF2E7D32),
                    start = Offset(centerX, potY - 40f),
                    end = Offset(centerX - 50f, potY - 70f),
                    strokeWidth = 10f
                )
                drawLine(
                    color = Color(0xFF2E7D32),
                    start = Offset(centerX, potY - 70f),
                    end = Offset(centerX + 40f, potY - 95f),
                    strokeWidth = 10f
                )
                drawOval(color = Color(0xFF4CAF50), topLeft = Offset(centerX - 65f, potY - 85f), size = Size(30f, 18f))
                drawOval(color = Color(0xFF4CAF50), topLeft = Offset(centerX + 30f, potY - 110f), size = Size(30f, 18f))
                drawOval(color = Color(0xFF4CAF50), topLeft = Offset(centerX - 35f, potY - 125f), size = Size(25f, 15f))

                val centerFlower = Offset(centerX, potY - 130f)
                drawCircle(color = Color(0xFFE91E63), radius = 10f, center = centerFlower)
                val petalRadius = 8f
                drawCircle(color = Color(0xFFF48FB1), radius = petalRadius, center = Offset(centerX - 12f, potY - 130f))
                drawCircle(color = Color(0xFFF48FB1), radius = petalRadius, center = Offset(centerX + 12f, potY - 130f))
                drawCircle(color = Color(0xFFF48FB1), radius = petalRadius, center = Offset(centerX, potY - 142f))
                drawCircle(color = Color(0xFFF48FB1), radius = petalRadius, center = Offset(centerX - 8f, potY - 118f))
                drawCircle(color = Color(0xFFF48FB1), radius = petalRadius, center = Offset(centerX + 8f, potY - 118f))
            }
            else -> {
                // BLOOMING TREE
                drawLine(
                    color = Color(0xFF8D6E63),
                    start = Offset(centerX, potY - 8f),
                    end = Offset(centerX, potY - 120f),
                    strokeWidth = 22f
                )
                val crownCenter = Offset(centerX, potY - 140f)
                drawCircle(color = Color(0xFF2E7D32), radius = 50f, center = crownCenter)
                drawCircle(color = Color(0xFF4CAF50), radius = 40f, center = Offset(centerX - 35f, potY - 140f))
                drawCircle(color = Color(0xFF4CAF50), radius = 40f, center = Offset(centerX + 35f, potY - 140f))
                drawCircle(color = Color(0xFF81C784), radius = 35f, center = Offset(centerX, potY - 170f))

                // Mini blossoms
                drawCircle(color = Color(0xFFFF80AB), radius = 7f, center = Offset(centerX - 20f, potY - 130f))
                drawCircle(color = Color(0xFFFF80AB), radius = 7f, center = Offset(centerX + 20f, potY - 140f))
                drawCircle(color = Color(0xFFFF80AB), radius = 7f, center = Offset(centerX - 5f, potY - 160f))
                drawCircle(color = Color(0xFFFFD54F), radius = 6f, center = Offset(centerX + 10f, potY - 120f))
            }
        }

        // Draw Terracotta pot body
        drawPath(
            path = potPath,
            color = Color(0xFFD84315)
        )
        // Pot rim
        drawRoundRect(
            color = Color(0xFFE64A19),
            topLeft = Offset(centerX - potWidthTop - 5f, potY),
            size = Size(potWidthTop * 2f + 10f, 14f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}
