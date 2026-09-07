package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object WalkingSession : Screen("walking_session")
    object Garden : Screen("garden")
    object AICoach : Screen("ai_coach")
    object Profile : Screen("profile")
    object ActivityHistory : Screen("activity_history")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Dashboard.route,
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        route = Screen.WalkingSession.route,
        title = "Session",
        selectedIcon = Icons.AutoMirrored.Filled.DirectionsWalk,
        unselectedIcon = Icons.AutoMirrored.Outlined.DirectionsWalk
    ),
    BottomNavItem(
        route = Screen.Garden.route,
        title = "Garden",
        selectedIcon = Icons.Filled.Spa,
        unselectedIcon = Icons.Outlined.Spa
    ),
    BottomNavItem(
        route = Screen.AICoach.route,
        title = "AI Coach",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)
