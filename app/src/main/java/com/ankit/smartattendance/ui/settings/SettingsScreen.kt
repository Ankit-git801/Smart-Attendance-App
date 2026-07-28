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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.ankit.smartattendance.viewmodel.AppViewModel

@Composable
fun SettingsScreen(navController: NavController, appViewModel: AppViewModel) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    val currentTheme by appViewModel.theme.collectAsStateWithLifecycle()
    val userName by appViewModel.userName.collectAsStateWithLifecycle()
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete Everything") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") } 
            }
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
                SettingsSectionTitle("Cloud Account")
                CloudSettingsItem(appViewModel)
            }
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
                SystemPermissionItem(
                    title = "Exact Alarms",
                    subtitle = "Ensure reminders fire on time",
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val intent = Intent().apply {
                                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
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
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onNameChange(name) })
            )
        },
        confirmButton = { 
            Button(
                onClick = { onNameChange(name) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") } 
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") } 
        }
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
        confirmButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") } 
        }
    )
}

@Composable
fun CloudSettingsItem(appViewModel: AppViewModel) {
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Listen for Auth changes automatically
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            user = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your local data will be cleared, but your cloud backup will remain safe.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        appViewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Sign Out") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") } 
            }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            appViewModel = appViewModel,
            onResult = { success, error ->
                if (success) {
                    showAuthDialog = false
                    errorMessage = null
                } else {
                    errorMessage = error
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Automatic Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            } else {
                val displayMessage = when {
                    user != null -> "Account: ${user?.email}. Data is automatically synced with the cloud."
                    else -> errorMessage ?: "Connect an account to backup your data across devices."
                }
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            
            if (user == null) {
                Button(
                    onClick = { showAuthDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Connect Account")
                }
            } else {
                OutlinedButton(
                    onClick = { 
                        showLogoutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    appViewModel: AppViewModel,
    initialIsSignUp: Boolean = false,
    onResult: (Boolean, String?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(initialIsSignUp) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSignUp) "Create Account" else "Sign In") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                
                TextButton(
                    onClick = { isSignUp = !isSignUp; error = null },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                }

                if (!isSignUp) {
                    TextButton(
                        onClick = {
                            if (email.isBlank()) {
                                error = "Enter your email to reset password"
                                return@TextButton
                            }
                            appViewModel.resetPassword(email) { success, msg ->
                                error = if (success) "Password reset email sent!" else msg
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please fill all fields"
                        return@Button
                    }
                    isLoading = true
                    if (isSignUp) {
                        appViewModel.signUpWithEmail(email, password) { success, msg ->
                            isLoading = false
                            if (success) {
                                onResult(true, null)
                            } else {
                                error = msg
                                onResult(false, msg)
                            }
                        }
                    } else {
                        appViewModel.loginWithEmail(email, password) { success, msg ->
                            isLoading = false
                            if (success) {
                                onResult(true, null)
                            } else {
                                // Map common errors
                                val finalMsg = when {
                                    msg?.contains("password", ignoreCase = true) == true || 
                                    msg?.contains("auth is incorrect", ignoreCase = true) == true ||
                                    msg?.contains("auth credential is incorrect", ignoreCase = true) == true ||
                                    msg?.contains("invalid-credential", ignoreCase = true) == true ||
                                    msg?.contains("invalid credential", ignoreCase = true) == true -> "Wrong password. Please try again."
                                    msg?.contains("user-not-found", ignoreCase = true) == true || msg?.contains("no user", ignoreCase = true) == true -> "No account found with this email."
                                    else -> msg ?: "Authentication failed"
                                }
                                error = finalMsg
                                onResult(false, finalMsg)
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (isSignUp) "Sign Up" else "Sign In")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss, 
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }
        }
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
