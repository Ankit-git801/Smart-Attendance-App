package com.ankit.smartattendance.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.delay

private fun getGreeting(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..23 -> "Good Evening"
        else -> "Hello"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsState()
    val userName by appViewModel.userName.collectAsState()

    // States to manage the two-step dialog process for adding an extra class.
    var showExtraClassConfirmationDialog by remember { mutableStateOf(false) }
    var showExtraClassSubjectDialog by remember { mutableStateOf(false) }

    // The first dialog: confirms if the user wants to add an extra class for today.
    if (showExtraClassConfirmationDialog) {
        ExtraClassConfirmationDialog(
            onDismiss = { showExtraClassConfirmationDialog = false },
            onConfirm = {
                showExtraClassConfirmationDialog = false
                showExtraClassSubjectDialog = true // If confirmed, open the second dialog.
            }
        )
    }

    // The second dialog: allows the user to select the subject for the extra class.
    if (showExtraClassSubjectDialog) {
        ExtraClassSubjectDialog(
            subjects = subjects,
            onDismiss = { showExtraClassSubjectDialog = false },
            onConfirm = { subjectId, isPresent ->
                appViewModel.markExtraClassAttendance(subjectId, isPresent, "Extra Class")
                showExtraClassSubjectDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GreetingCard(userName = userName)
            Spacer(modifier = Modifier.height(24.dp))
            QuickActions(
                onExtraClassClick = { showExtraClassConfirmationDialog = true }, // This now triggers the confirmation dialog.
                onNewSubjectClick = { navController.navigate("add_subject") }
            )
        }

        item {
            Text(
                text = "TODAY'S CLASSES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (todaysSchedule.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.WbSunny,
                    title = "No Classes Today",
                    subtitle = "You have no classes scheduled for today. Enjoy your day off!"
                )
            }
        } else {
            itemsIndexed(todaysSchedule, key = { _, item -> "schedule_${item.schedule.id}" }) { index, scheduleWithSubject ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    TodayScheduleCard(
                        scheduleWithSubject = scheduleWithSubject,
                        appViewModel = appViewModel,
                        onMark = { isPresent ->
                            appViewModel.markAttendance(
                                scheduleWithSubject.subject.id,
                                scheduleWithSubject.schedule.id,
                                isPresent
                            )
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ALL SUBJECTS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (subjects.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.School,
                    title = "No Subjects Added",
                    subtitle = "Get started by adding your first subject. You can set a custom name, color, and attendance target for it.",
                    buttonText = "Add New Subject",
                    onButtonClick = { navController.navigate("add_subject") }
                )
            }
        } else {
            itemsIndexed(subjects, key = { _, item -> "subject_${item.id}" }) { index, subject ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 50 * index)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(300, delayMillis = 50 * index)
                    ),
                    exit = fadeOut()
                ) {
                    SubjectCard(subject = subject, appViewModel = appViewModel) {
                        navController.navigate("subject_detail/${subject.id}")
                    }
                }
            }
        }
    }
}

// This is the new confirmation dialog.
@Composable
private fun ExtraClassConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Extra Class") },
        text = { Text("Do you want to mark an extra class for today, $formattedDate?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
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
fun GreetingCard(userName: String) {
    val greeting = getGreeting()
    val icon = when {
        greeting.contains("Morning") -> Icons.Outlined.WbSunny
        greeting.contains("Afternoon") -> Icons.Outlined.Brightness5
        else -> Icons.Outlined.NightsStay
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Greeting Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "$greeting, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onButtonClick) {
                    Text(buttonText)
                }
            }
        }
    }
}


@Composable
private fun QuickActions(onExtraClassClick: () -> Unit, onNewSubjectClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onExtraClassClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add Extra Class")
                Spacer(Modifier.width(8.dp))
                Text("Extra Class", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onNewSubjectClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Subject")
                Spacer(Modifier.width(8.dp))
                Text("New Subject", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Renamed for clarity, this is the dialog for selecting the subject.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraClassSubjectDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, isPresent: Boolean) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Subject for Extra Class") },
        text = {
            Column {
                if (subjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = subjects.find { it.id == selectedSubjectId }?.name
                                ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        selectedSubjectId = subject.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("Please add a subject first.")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, true) }
                    },
                    enabled = selectedSubjectId != null
                ) {
                    Text("Present")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, false) }
                    },
                    enabled = selectedSubjectId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Absent")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScheduleCard(
    scheduleWithSubject: ScheduleWithSubject,
    appViewModel: AppViewModel = viewModel(),
    onMark: (Boolean) -> Unit
) {
    val records by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())
    var isAlreadyMarked by remember { mutableStateOf(false) }
    var wasPresent by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(records, scheduleWithSubject.subject.id) {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val recordForToday = records.find { it.subjectId == scheduleWithSubject.subject.id && it.date == todayEpochDay }
        isAlreadyMarked = recordForToday != null
        wasPresent = recordForToday?.isPresent
    }

    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = formatTime(schedule.startHour, schedule.startMinute)
    val endTime = formatTime(schedule.endHour, schedule.endMinute)
    val subjectColor = Color(android.graphics.Color.parseColor(subject.color))
    val isLive = scheduleWithSubject.isCurrentClass

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "$startTime - $endTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isLive && !isAlreadyMarked) {
                        LiveBadge()
                    }
                }

                AnimatedContent(
                    targetState = isAlreadyMarked,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300)) using
                                SizeTransform(clip = false)
                    },
                    modifier = Modifier.align(Alignment.End), label = ""
                ) { marked ->
                    if (marked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (wasPresent == true) Icons.Filled.CheckCircle else Icons.Filled.Cancel
                            val text = if (wasPresent == true) "Marked as Present" else "Marked as Absent"
                            val color = if (wasPresent == true) SuccessGreen else ErrorRed
                            Icon(icon, contentDescription = null, tint = color)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Mark as:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = { onMark(true) }) { Text("Present") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { onMark(false) }) { Text("Absent") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_badge_transition")
    val colorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "live_badge_alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(ErrorRed.copy(alpha = colorAlpha))
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}

@Composable
fun SubjectCard(subject: Subject, appViewModel: AppViewModel, onClick: () -> Unit) {
    var percentage by remember { mutableStateOf<Double?>(null) }
    val subjectColor = Color(android.graphics.Color.parseColor(subject.color))

    LaunchedEffect(subject.id, appViewModel.allAttendanceRecords) {
        percentage = appViewModel.getAttendancePercentage(subject.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Target: ${subject.targetAttendance}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            percentage?.let {
                val animatedPercentage by animateFloatAsState(
                    targetValue = it.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = ""
                )
                AnimatedCircularProgress(
                    percentage = animatedPercentage,
                    color = subjectColor
                )
            }
        }
    }
}

@Composable
fun AnimatedCircularProgress(
    percentage: Float,
    color: Color,
    radius: Dp = 32.dp,
    strokeWidth: Dp = 4.dp
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (percentage / 100) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}
