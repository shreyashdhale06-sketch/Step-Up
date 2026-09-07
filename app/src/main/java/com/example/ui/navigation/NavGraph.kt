package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.ui.screens.*
import com.example.ui.viewmodel.StepViewModel

@Composable
fun AppNavigation(
    viewModel: StepViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on major functional tab screens
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.WalkingSession.route,
        Screen.Garden.route,
        Screen.AICoach.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(viewModel) { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            
            composable(Screen.Login.route) {
                LoginScreen(viewModel) { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHistory = { navController.navigate(Screen.ActivityHistory.route) },
                    onNavigateToGarden = { navController.navigate(Screen.Garden.route) },
                    onNavigateToCoach = { navController.navigate(Screen.AICoach.route) }
                )
            }
            
            composable(Screen.WalkingSession.route) {
                WalkingSessionScreen(viewModel)
            }
            
            composable(Screen.Garden.route) {
                GardenScreen(viewModel)
            }

            composable(Screen.AICoach.route) {
                AICoachScreen(viewModel)
            }
            
            composable(Screen.ActivityHistory.route) {
                ActivityHistoryScreen(viewModel)
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel) {
                    navController.navigate(Screen.Settings.route)
                }
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
