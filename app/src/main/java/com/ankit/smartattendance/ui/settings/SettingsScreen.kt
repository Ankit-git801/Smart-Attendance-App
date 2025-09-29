@file:OptIn(ExperimentalMaterial3Api::class)

package com.ankit.smartattendance.ui.settings

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
import androidx.navigation.NavController
import com.ankit.smartattendance.viewmodel.AppViewModel

@Composable
fun SettingsScreen(navController: NavController, appViewModel: AppViewModel) {
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
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
            title = { Text("Delete All Data?") },
            text = { Text("This will permanently delete all subjects, schedules, and attendance records, including holidays. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.deleteAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
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
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                SettingsSectionTitle("Personalization")
                SettingsItem(
                    title = "User Name",
                    subtitle = userName,
                    icon = { Icon(Icons.Default.Person, contentDescription = "User Name") },
                    onClick = { showNameDialog = true }
                )
                SettingsItem(
                    title = "Theme",
                    subtitle = currentTheme,
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Theme") },
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsSectionTitle("General")
                SettingsItem(
                    title = "Weekly Schedule",
                    subtitle = "View all your classes for the week",
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Weekly Schedule") },
                    onClick = { navController.navigate("weekly_schedule") }
                )
            }
            item {
                SettingsSectionTitle("Data Management")
                SettingsItem(
                    title = "Delete All Data",
                    subtitle = "Remove all subjects and records",
                    isDestructive = true,
                    icon = {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "Delete Data",
                        )
                    },
                    onClick = { showDeleteDialog = true }
                )
            }
            item {
                SettingsSectionTitle("System & Permissions")
                SystemPermissionItem(
                    title = "Battery Optimization",
                    subtitle = "Required for reliable notifications",
                    onClick = {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

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
        confirmButton = { Button(onClick = { onNameChange(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ThemeDialog(currentTheme: String, onDismiss: () -> Unit, onThemeSelected: (String) -> Unit) {
    val themes = listOf("System Default", "Light", "Dark")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(theme, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else LocalContentColor.current
    val subtitleColor = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SystemPermissionItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.OpenInNew, contentDescription = "Open Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
