package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StepViewModel

@Composable
fun ProfileScreen(
    viewModel: StepViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Profile Flows
    val userName by viewModel.userName.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val userWeight by viewModel.userWeight.collectAsState()
    val userHeight by viewModel.userHeight.collectAsState()
    val userStrideLength by viewModel.userStrideLength.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val allTimeHighestSteps by viewModel.allTimeHighestSteps.collectAsState()
    
    // Database totals for profile stats card
    val dailyRecords by viewModel.dailyRecords.collectAsState()
    val workouts by viewModel.walkingSessions.collectAsState()

    // All-time aggregate calculations
    val allTimeSteps = dailyRecords.sumOf { it.steps }
    val totalWorkouts = workouts.size

    // Form states initialized with database values
    var nameEdit by remember { mutableStateOf("") }
    var goalEdit by remember { mutableStateOf("") }
    var weightEdit by remember { mutableStateOf("") }
    var heightEdit by remember { mutableStateOf("") }
    var strideEdit by remember { mutableStateOf("") }

    // Synchronize form edits when flow updates from datastore
    LaunchedEffect(userName, dailyStepGoal, userWeight, userHeight, userStrideLength) {
        nameEdit = userName
        goalEdit = dailyStepGoal.toString()
        weightEdit = userWeight.toInt().toString()
        heightEdit = userHeight.toInt().toString()
        strideEdit = userStrideLength.toInt().toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("settings_button_profile")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Profile Avatar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Active StepUp Member • $currentStreak-Day Streak 🔥",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // All Time Records & Statistics
        Text(
            text = "Lifetime Records & Stats",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%,d", allTimeHighestSteps),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Single-Day Record", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%,d", allTimeSteps),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Lifetime Steps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Editable Form
        Text(
            text = "Body Metrics & Step Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Text(
            text = "Changing weight or stride length immediately recalculates all dashboard and session metrics.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = nameEdit,
                    onValueChange = { nameEdit = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Goal Field
                OutlinedTextField(
                    value = goalEdit,
                    onValueChange = { goalEdit = it },
                    label = { Text("Daily Step Goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Height Field
                    OutlinedTextField(
                        value = heightEdit,
                        onValueChange = { 
                            heightEdit = it 
                            val h = it.toFloatOrNull()
                            if (h != null && h > 50f) {
                                strideEdit = (h * 0.414f).toInt().toString()
                            }
                        },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Weight Field
                    OutlinedTextField(
                        value = weightEdit,
                        onValueChange = { weightEdit = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Stride Length Field
                OutlinedTextField(
                    value = strideEdit,
                    onValueChange = { strideEdit = it },
                    label = { Text("Stride Length (cm)") },
                    supportingText = { Text("Default: Height × 0.414 = ${(userHeight * 0.414f).toInt()} cm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Save button
                Button(
                    onClick = {
                        val parsedGoal = goalEdit.toIntOrNull()
                        val parsedHeight = heightEdit.toFloatOrNull()
                        val parsedWeight = weightEdit.toFloatOrNull()
                        val parsedStride = strideEdit.toFloatOrNull() ?: (parsedHeight?.times(0.414f) ?: 72.5f)

                        if (nameEdit.trim().isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                        } else if (parsedGoal == null || parsedGoal <= 0) {
                            Toast.makeText(context, "Please enter a valid step goal!", Toast.LENGTH_SHORT).show()
                        } else if (parsedHeight == null || parsedHeight <= 0f) {
                            Toast.makeText(context, "Please enter a valid height!", Toast.LENGTH_SHORT).show()
                        } else if (parsedWeight == null || parsedWeight <= 0f) {
                            Toast.makeText(context, "Please enter a valid weight!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveProfile(
                                name = nameEdit.trim(),
                                goal = parsedGoal,
                                weight = parsedWeight,
                                height = parsedHeight,
                                strideLength = parsedStride
                            )
                            Toast.makeText(context, "Profile updated & metrics recalculated! ✨", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile & Recalculate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discreet Web Preview / Hardware Test Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hardware Test Action",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Trigger +10 physical steps to test dashboard, garden, and active session sync.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        viewModel.recordSensorSteps(10)
                        Toast.makeText(context, "+10 Steps Counted!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Test Step (+10)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
