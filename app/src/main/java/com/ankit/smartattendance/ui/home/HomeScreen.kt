package com.ankit.smartattendance.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

private fun getGreeting(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..23 -> "Good Evening"
        else -> "Hello"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsState()
    val showExtraClassDialog by appViewModel.showExtraClassDialog.collectAsState()
    val userName by appViewModel.userName.collectAsState()

    if (showExtraClassDialog) {
        ExtraClassDialog(
            subjects = subjects,
            onDismiss = { appViewModel.hideExtraClassDialog() },
            onConfirm = { subjectId, isPresent ->
                appViewModel.markExtraClassAttendance(subjectId, isPresent, "Extra Class")
                appViewModel.hideExtraClassDialog()
            }
        )
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Greeting and Date
                Text(
                    text = "${getGreeting()}, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Quick Action Buttons
                QuickActions(
                    onExtraClassClick = { appViewModel.showExtraClassDialog() },
                    onNewSubjectClick = { navController.navigate("add_subject") }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TODAY'S CLASSES",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (todaysSchedule.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No classes scheduled for today.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(todaysSchedule, key = { "schedule_${it.schedule.id}" }) { scheduleWithSubject ->
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ALL SUBJECTS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subjects.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No subjects yet. Tap 'New Subject' above to add one.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(subjects, key = { "subject_${it.id}" }) { subject ->
                    SubjectCard(subject = subject, appViewModel = appViewModel) {
                        navController.navigate("subject_detail/${subject.id}")
                    }
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
            // Extra Class Button (Tonal)
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
                Text("Extra Class", fontWeight = FontWeight.Bold)
            }
            // New Subject Button (Filled)
            Button(
                onClick = onNewSubjectClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Subject")
                Spacer(Modifier.width(8.dp))
                Text("New Subject", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraClassDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, isPresent: Boolean) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Extra Class") },
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

@Composable
fun TodayScheduleCard(
    scheduleWithSubject: ScheduleWithSubject,
    appViewModel: AppViewModel = viewModel(),
    onMark: (Boolean) -> Unit
) {
    val records by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())

    val isAlreadyMarked = remember(records, scheduleWithSubject.schedule.id) {
        val todayEpochDay = LocalDate.now().toEpochDay()
        records.any { record ->
            record.scheduleId == scheduleWithSubject.schedule.id && record.date == todayEpochDay
        }
    }

    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = formatTime(schedule.startHour, schedule.startMinute)
    val endTime = formatTime(schedule.endHour, schedule.endMinute)
    val subjectColor = Color(android.graphics.Color.parseColor(subject.color))
    val isLive = scheduleWithSubject.isCurrentClass

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(subjectColor)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$startTime - $endTime",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isLive) {
                    LiveBadge()
                }
            }

            if (!scheduleWithSubject.isCompleted && !isAlreadyMarked) {
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
                    Button(onClick = { onMark(true) }) {
                        Text("Present")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onMark(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Absent")
                    }
                }
            } else if (isAlreadyMarked) {
                Text(
                    text = "Attendance Marked",
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_badge_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "live_badge_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(ErrorRed)
        )
        Text(
            text = "LIVE",
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
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

    // Re-calculates percentage whenever the subject or its records change
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(subject.color)))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: ${subject.targetAttendance}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            percentage?.let {
                val percentageColor =
                    if (it >= subject.targetAttendance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    text = "${"%.1f".format(it)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = percentageColor
                )
            }
        }
    }
}
