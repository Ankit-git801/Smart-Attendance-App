package com.ankit.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ankit.smartattendance.ui.addsubject.AddSubjectScreen
import com.ankit.smartattendance.ui.calendar.CalendarScreen
import com.ankit.smartattendance.ui.home.HomeScreen
import com.ankit.smartattendance.ui.settings.SettingsScreen
import com.ankit.smartattendance.ui.statistics.StatisticsScreen
import com.ankit.smartattendance.ui.subjectdetail.SubjectDetailScreen
import com.ankit.smartattendance.ui.theme.SmartAttendanceTheme
import com.ankit.smartattendance.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartAttendanceTheme {
                SmartAttendanceApp()
            }
        }
    }
}

@Composable
fun SmartAttendanceApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()

    // Determine if the FAB should be shown based on the current screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showFab = currentRoute == BottomNavItem.Home.route

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_subject") }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Subject")
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            appViewModel = appViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Calendar.route) {
            CalendarScreen(appViewModel = appViewModel)
        }
        composable(BottomNavItem.Statistics.route) {
            StatisticsScreen(appViewModel = appViewModel)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(appViewModel = appViewModel)
        }
        composable("add_subject") {
            AddSubjectScreen(navController = navController, appViewModel = appViewModel)
        }
        composable("edit_subject/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")?.toLongOrNull() ?: 0L
            AddSubjectScreen(navController = navController, subjectId = subjectId, appViewModel = appViewModel)
        }
        composable("subject_detail/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            requireNotNull(subjectId) { "Subject ID is required" }
            SubjectDetailScreen(subjectId.toLong(), navController, appViewModel)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Statistics,
        BottomNavItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Calendar : BottomNavItem("calendar", "Calendar", Icons.Default.Today)
    object Statistics : BottomNavItem("statistics", "Stats", Icons.Default.Analytics)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}
