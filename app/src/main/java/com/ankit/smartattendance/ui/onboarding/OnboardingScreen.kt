package com.ankit.smartattendance.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankit.smartattendance.ui.theme.PoppinsFamily
import com.ankit.smartattendance.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(appViewModel: AppViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible.value = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(1000)) + expandVertically(tween(1000))
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200)) { 40 }
            ) {
                Text(
                    text = "Welcome to\nSmart Attendance",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = PoppinsFamily,
                    lineHeight = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400)) { 40 }
            ) {
                Text(
                    text = "Let's personalize your experience. What should we call you?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600)) { 40 }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enter Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            appViewModel.completeOnboarding(name)
                            onComplete()
                        }
                    })
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(1000, 800))
            ) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            appViewModel.completeOnboarding(name)
                            onComplete()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
