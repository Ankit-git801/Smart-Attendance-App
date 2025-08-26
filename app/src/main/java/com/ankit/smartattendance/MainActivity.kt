package com.ankit.smartattendance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.ankit.smartattendance.utils.NotificationHelper
import com.ankit.smartattendance.viewmodel.AppViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val theme by appViewModel.theme.collectAsState()
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            SmartAttendanceTheme(darkTheme = useDarkTheme) {
                RequestPermissions()
                SmartAttendanceApp(appViewModel = appViewModel)
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    RequestNotificationPermission()
    RequestBatteryOptimizationPermission()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState =
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
fun RequestBatteryOptimizationPermission() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Battery Optimization") },
            text = { Text("To ensure that attendance reminders work correctly, please allow the app to run in the background without battery restrictions.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SmartAttendanceApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // List of top-level destinations where the bottom bar should be visible
    val topLevelDestinations = listOf(
        BottomNavItem.Home.route,
        BottomNavItem.Calendar.route,
        BottomNavItem.Statistics.route,
        BottomNavItem.Settings.route
    )
    val showBottomBar = topLevelDestinations.any { it == currentDestination?.route }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
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
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(350)) },
        exitTransition = { fadeOut(animationSpec = tween(350)) },
        popEnterTransition = { fadeIn(animationSpec = tween(350)) },
        popExitTransition = { fadeOut(animationSpec = tween(350)) }
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Calendar.route) {
            CalendarScreen(appViewModel = appViewModel)
        }
        composable(BottomNavItem.Statistics.route) {
            StatisticsScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(appViewModel = appViewModel)
        }
        composable("add_subject") {
            AddSubjectScreen(navController = navController, appViewModel = appViewModel)
        }
        composable("edit_subject/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")?.toLongOrNull() ?: 0L
            AddSubjectScreen(
                navController = navController,
                subjectId = subjectId,
                appViewModel = appViewModel
            )
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
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Calendar :
        BottomNavItem("calendar", "Calendar", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)

    object Statistics :
        BottomNavItem("statistics", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart)

    object Settings :
        BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
