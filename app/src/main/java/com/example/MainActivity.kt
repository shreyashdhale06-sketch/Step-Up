package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.StepUpTheme
import com.example.ui.viewmodel.StepViewModel

class MainActivity : ComponentActivity() {
  private lateinit var stepViewModel: StepViewModel

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted && ::stepViewModel.isInitialized) {
      // Restart tracking with the newly granted high-accuracy sensor
      stepViewModel.stopSensorTracking()
      stepViewModel.startSensorTracking()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Request step counter permission on Android 10+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
      }
    }

    setContent {
      val viewModel: StepViewModel = viewModel()
      stepViewModel = viewModel
      val darkThemePreference by viewModel.darkTheme.collectAsState()
      
      // Auto-Request Activity Recognition Permission on Android 10+
      val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        if (isGranted) {
          viewModel.stopSensorTracking()
          viewModel.startSensorTracking()
        }
      }

      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
          }
        }
      }

      // Lifecycle-Aware Sensor Registration across all Compose tab navigation
      DisposableEffect(Unit) {
        viewModel.startSensorTracking()
        onDispose {
          viewModel.stopSensorTracking()
        }
      }

      // Choose theme mode dynamically based on preference, falling back to system default
      val isDarkTheme = darkThemePreference ?: isSystemInDarkTheme()

      StepUpTheme(darkTheme = isDarkTheme) {
        AppNavigation(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (::stepViewModel.isInitialized) {
      stepViewModel.startSensorTracking()
    }
  }

  override fun onPause() {
    super.onPause()
    if (::stepViewModel.isInitialized) {
      stepViewModel.stopSensorTracking()
    }
  }
}
