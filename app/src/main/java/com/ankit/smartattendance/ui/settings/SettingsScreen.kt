package com.ankit.smartattendance.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ankit.smartattendance.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appViewModel: AppViewModel) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentTheme by appViewModel.theme.collectAsState()
    val context = LocalContext.current

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { appViewModel.setTheme(it) }
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

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Appearance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingsItem(
                    title = "Theme",
                    subtitle = currentTheme,
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Theme") },
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                Text("Data Management", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
                SettingsItem(
                    title = "Delete All Data",
                    subtitle = "Remove all subjects and records",
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Delete Data", tint = MaterialTheme.colorScheme.error) },
                    onClick = { showDeleteDialog = true }
                )
            }
            item {
                Text("System", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
                BatteryOptimizationSetting(context = context)
            }
        }
    }
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
fun SettingsItem(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = {
                                onThemeSelected(theme)
                                onDismiss()
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
