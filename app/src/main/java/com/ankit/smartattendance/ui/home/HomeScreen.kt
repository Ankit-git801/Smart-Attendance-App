package com.ankit.smartattendance.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsState()
    val showExtraClassDialog by appViewModel.showExtraClassDialog.collectAsState()

    val today = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    if (showExtraClassDialog) {
        ExtraClassDialog(
            subjects = subjects,
            onDismiss = { appViewModel.hideExtraClassDialog() },
            onConfirm = { subjectId, isPresent ->
                appViewModel.markExtraClassAttendance(subjectId, isPresent, "Extra Class")
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            // CORRECTED: Replaced `spacing` with `verticalArrangement`
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { appViewModel.showExtraClassDialog() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Extra Class")
                }
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_subject") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Subject") },
                    text = { Text("New Subject") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = today,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Today's Schedule")
            }

            if (todaysSchedule.isEmpty()) {
                item {
                    EmptyState(
                        message = "No classes scheduled for today. Enjoy your day!",
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(todaysSchedule, key = { "schedule_${it.schedule.id}" }) { scheduleWithSubject ->
                    TodayScheduleCard(
                        scheduleWithSubject = scheduleWithSubject,
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
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "All Subjects")
            }

            if (subjects.isEmpty()) {
                item {
                    EmptyState(
                        message = "No subjects yet. Tap 'New Subject' to add one.",
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
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
            if (subjects.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
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
                Text("Please add a subject first to mark extra attendance.")
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, false) }
                        onDismiss()
                    },
                    enabled = selectedSubjectId != null
                ) { Text("Absent") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, true) }
                        onDismiss()
                    },
                    enabled = selectedSubjectId != null
                ) { Text("Present") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TodayScheduleCard(scheduleWithSubject: ScheduleWithSubject, onMark: (Boolean) -> Unit) {
    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = formatTime(schedule.startHour, schedule.startMinute)
    val endTime = formatTime(schedule.endHour, schedule.endMinute)

    val isCompleted by remember(scheduleWithSubject) { derivedStateOf { scheduleWithSubject.isCompleted } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Using a consistent color scheme
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.name.first().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = subject.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$startTime - $endTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Buttons are now only shown if the class is not completed
            AnimatedVisibility(visible = !isCompleted) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = { onMark(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Present")
                    }
                    OutlinedButton(
                        onClick = { onMark(false) }
                    ) {
                        Text("Absent")
                    }
                }
            }
        }
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

    LaunchedEffect(subject.id) {
        percentage = appViewModel.getAttendancePercentage(subject.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(subject.color)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subject.name.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(android.graphics.Color.parseColor(subject.color))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Target: ${subject.targetAttendance}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            percentage?.let {
                val percentageColor = if (it >= subject.targetAttendance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    text = "${"%.1f".format(it)}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = percentageColor
                )
            }
        }
    }
}
