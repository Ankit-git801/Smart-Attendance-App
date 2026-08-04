/*
 * Copyright (c) 2026 Ankit. All rights reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.ankit.attendwise

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ankit.attendwise.ui.addsubject.AddSubjectScreen
import com.ankit.attendwise.ui.calendar.CalendarScreen
import com.ankit.attendwise.ui.home.HomeScreen
import com.ankit.attendwise.ui.onboarding.OnboardingScreen
import com.ankit.attendwise.ui.settings.SettingsScreen
import com.ankit.attendwise.ui.statistics.StatisticsScreen
import com.ankit.attendwise.ui.subjectdetail.SubjectDetailScreen
import com.ankit.attendwise.ui.theme.AttendWiseTheme
import com.ankit.attendwise.ui.weeklysched.WeeklyScheduleScreen
import com.ankit.attendwise.utils.NotificationHelper
import com.ankit.attendwise.viewmodel.AppViewModel
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        
        viewModel = androidx.lifecycle.ViewModelProvider(this)[AppViewModel::class.java]

        setContent {
            val appViewModel = viewModel
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                appViewModel.attendanceActionFeedback.collectLatest { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            
            // Keep the splash screen on screen until the onboarding state is loaded
            splashScreen.setKeepOnScreenCondition {
                appViewModel.isOnboardingComplete.value == null
            }

            val theme by appViewModel.theme.collectAsStateWithLifecycle()
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            AttendWiseTheme(darkTheme = useDarkTheme) {
                RequestAllPermissions()
                
                val updateAvailable by appViewModel.updateAvailable.collectAsStateWithLifecycle()
                val isForceUpdate by appViewModel.isForceUpdate.collectAsStateWithLifecycle()
                var showUpdateDialog by remember { mutableStateOf(false) }
                
                LaunchedEffect(updateAvailable) {
                    if (updateAvailable) showUpdateDialog = true
                }

                if (showUpdateDialog) {
                    UpdateDialog(
                        isForceUpdate = isForceUpdate,
                        onDismiss = { if (!isForceUpdate) showUpdateDialog = false }
                    )
                }

                AttendWiseApp(appViewModel = appViewModel)
            }
        }
        
        // Initial check for intent when app starts
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val subjectId = intent?.getStringExtra("subject_id")
        if (!subjectId.isNullOrEmpty()) {
            if (::viewModel.isInitialized) {
                viewModel.triggerNavigation(subjectId)
                intent.removeExtra("subject_id")
            }
        }
    }
}

@Composable
fun UpdateDialog(isForceUpdate: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isForceUpdate) "Update Required" else "Update Available") },
        text = { 
            Text(
                if (isForceUpdate) 
                    "This version of AttendWise is no longer supported. Please update to the latest version from the Play Store to continue."
                else 
                    "A newer version of AttendWise is available. Please update to get the latest features and bug fixes."
            ) 
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    setPackage("com.android.vending")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                }
            }) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!isForceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}

@Composable
fun RequestAllPermissions() {
    RequestNotificationPermission()
    RequestExactAlarmPermission()
    RequestBatteryOptimizationPermission()
    // DEFINITIVE FIX: Adding a specific dialog for manufacturer optimizations.
    RequestManufacturerBatteryOptimization()
}

@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED
            )
        }
        var showRationale by remember { mutableStateOf(false) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                hasPermission = isGranted
            }
        )

        LaunchedEffect(hasPermission) {
            if (!hasPermission) {
                showRationale = true
            }
        }

        if (showRationale) {
            AlertDialog(
                onDismissRequest = { showRationale = false },
                title = { Text(stringResource(R.string.perm_notifications_rationale_title)) },
                text = { Text(stringResource(R.string.perm_notifications_rationale_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRationale = false
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRationale = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun RequestExactAlarmPermission() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Permission Required") },
                    text = { Text("This app needs permission to set precise alarms for class reminders. Please enable 'Alarms & reminders' in the settings.") },
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                                context.startActivity(this)
                            }
                        }) { Text("Open Settings") }
                    }
                )
            }
        }
    }
}

@Composable
fun RequestBatteryOptimizationPermission() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        var showDialog by remember { mutableStateOf(true) }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Disable Battery Optimization") },
                text = { Text("To ensure reminders work reliably, please disable battery optimization for this app.") },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }) { Text("Allow") }
                }
            )
        }
    }
}

@Composable
fun RequestManufacturerBatteryOptimization() {
    val context = LocalContext.current
    val manufacturer = Build.MANUFACTURER.lowercase()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    var showDialog by remember { mutableStateOf(false) }

    val intent = remember {
        when {
            manufacturer == "oneplus" -> Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
            manufacturer == "oppo" -> Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
            manufacturer == "vivo" -> Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            manufacturer == "xiaomi" -> Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            manufacturer == "samsung" -> Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            else -> null
        }
    }

    if (intent != null) {
        LaunchedEffect(Unit) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Additional Step Required") },
            text = { 
                val manufacturerName = manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                Text("Your $manufacturerName device has aggressive battery optimizations that may prevent notifications. Please find 'AttendWise' in the list and enable 'Allow auto-launch' or 'Run in background' to ensure reminders work correctly.") 
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback if the specific activity is not found
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (e2: Exception) {}
                    }
                }) { Text("Open App Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("I've already done this")
                }
            }
        )
    }
}


// ... (The rest of your MainActivity.kt remains exactly the same)
// AttendWiseApp, AppNavigation, BottomNavigationBar, and BottomNavItem.

@Composable
fun AttendWiseApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnboardingComplete by appViewModel.isOnboardingComplete.collectAsStateWithLifecycle()

    // ATTENDWISE NAV: Handle navigation from notifications with race condition safety
    // The event is stored in a buffered Channel in ViewModel until collected here
    LaunchedEffect(isOnboardingComplete) {
        if (isOnboardingComplete == true) {
            appViewModel.navigationEvents.collect { subjectId ->
                try {
                    navController.navigate("subject_detail/$subjectId") {
                        launchSingleTop = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Navigation failed: ${e.message}")
                }
            }
        }
    }

    if (isOnboardingComplete == null) return

    val topLevelDestinations = listOf(
        BottomNavItem.Home.route,
        BottomNavItem.Calendar.route,
        BottomNavItem.Statistics.route,
        BottomNavItem.Settings.route
    )
    val showBottomBar = topLevelDestinations.any { it == currentDestination?.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar && isOnboardingComplete == true) {
                BottomNavigationBar(navController = navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            appViewModel = appViewModel,
            isOnboardingComplete = isOnboardingComplete == true,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    appViewModel: AppViewModel,
    isOnboardingComplete: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (isOnboardingComplete) BottomNavItem.Home.route else "onboarding",
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) }
    ) {
        composable("onboarding") {
            OnboardingScreen(appViewModel = appViewModel, onComplete = {
                navController.navigate(BottomNavItem.Home.route) {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Calendar.route) {
            CalendarScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Statistics.route) {
            StatisticsScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(navController = navController, appViewModel = appViewModel)
        }
        composable("add_subject") {
            AddSubjectScreen(navController = navController, appViewModel = appViewModel)
        }
        composable(
            route = "edit_subject/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            AddSubjectScreen(
                navController = navController,
                subjectId = subjectId,
                appViewModel = appViewModel
            )
        }
        composable(
            route = "subject_detail/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(subjectId, navController, appViewModel)
        }
        composable("weekly_schedule") {
            WeeklyScheduleScreen(navController = navController, appViewModel = appViewModel)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Statistics,
        BottomNavItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
    ) {
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
