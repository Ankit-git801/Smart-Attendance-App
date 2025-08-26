package com.ankit.smartattendance.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ankit.smartattendance.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appViewModel: AppViewModel) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    val currentTheme by appViewModel.theme.collectAsState()
    val userName by appViewModel.userName.collectAsState()
    val context = LocalContext.current

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = {
                appViewModel.setTheme(it)
                showThemeDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data") },
            text = { Text("Are you sure you want to delete everything? This will remove all subjects and attendance records permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.deleteAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton({ showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNameDialog) {
        UserNameDialog(
            currentName = userName,
            onDismiss = { showNameDialog = false },
            onNameChange = {
                appViewModel.setUserName(it)
                showNameDialog = false
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues), // Use padding from Scaffold
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Personalization",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsItem(
                    title = "User Name",
                    subtitle = userName,
                    icon = { Icon(Icons.Default.Person, contentDescription = "User Name") },
                    onClick = { showNameDialog = true }
                )
            }
            item {
                Text(
                    "Appearance",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                SettingsItem(
                    title = "Theme",
                    subtitle = currentTheme,
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Theme") },
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                Text(
                    "Data Management",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                SettingsItem(
                    title = "Delete All Data",
                    subtitle = "Remove all subjects and records",
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Data",
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { showDeleteDialog = true }
                )
            }
            item {
                Text(
                    "System Permissions",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                BatteryOptimizationSetting(context = context)
            }
            item {
                ExactAlarmSetting(context = context)
            }
            item {
                FullScreenIntentSetting(context = context)
            }
        }
    }
}
// ... (The rest of your SettingsScreen.kt code remains the same)
@Composable
fun UserNameDialog(currentName: String, onDismiss: () -> Unit, onNameChange: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Your Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onNameChange(name) })
            )
        },
        confirmButton = {
            Button(onClick = { onNameChange(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BatteryOptimizationSetting(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    SettingsItem(
        title = "Battery Optimization",
        subtitle = if (isIgnoringOptimizations) "Unrestricted" else "Optimized (Recommended to change)",
        icon = { Icon(Icons.Default.Info, contentDescription = "Battery Optimization") },
        onClick = {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    )
}

@Composable
fun ExactAlarmSetting(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canSchedule = alarmManager.canScheduleExactAlarms()
        SettingsItem(
            title = "Exact Alarm Permission",
            subtitle = if (canSchedule) "Allowed" else "Needed for reminders",
            icon = { Icon(Icons.Default.Info, "Exact Alarms") },
            onClick = {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(this)
                }
            }
        )
    }
}

@Composable
fun FullScreenIntentSetting(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canUse = notificationManager.canUseFullScreenIntent()
        SettingsItem(
            title = "Full-Screen Notifications",
            subtitle = if (canUse) "Allowed" else "Needed for on-screen prompts",
            icon = { Icon(Icons.Default.Info, "Full-Screen Notifications") },
            onClick = {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(this)
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun ThemeDialog(currentTheme: String, onDismiss: () -> Unit, onThemeSelected: (String) -> Unit) {
    val themes = listOf("Light", "Dark", "System Default")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(theme)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = {
                                onThemeSelected(theme)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(theme)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
