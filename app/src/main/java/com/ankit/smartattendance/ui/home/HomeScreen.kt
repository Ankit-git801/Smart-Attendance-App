package com.ankit.smartattendance.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

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
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_subject") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = { appViewModel.showExtraClassDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Extra Class") },
                    text = { Text("Extra Class") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = today,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TODAY'S CLASSES",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (todaysSchedule.isEmpty()) {
                item {
                    Text(
                        "No classes scheduled for today.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ALL SUBJECTS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (subjects.isEmpty()) {
                item {
                    Text(
                        "No subjects yet. Tap the '+' button to add your first subject.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
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
                        TextField(
                            value = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
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
            Row {
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
fun TodayScheduleCard(scheduleWithSubject: ScheduleWithSubject, onMark: (Boolean) -> Unit) {
    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = formatTime(schedule.startHour, schedule.startMinute)
    val endTime = formatTime(schedule.endHour, schedule.endMinute)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(text = subject.name, fontWeight = FontWeight.Bold)
                    Text(text = "$startTime - $endTime")
                }
            }
            Spacer(Modifier.height(12.dp))
            // -- CHANGE HERE: Buttons are now only shown if the class is not completed --
            if (!scheduleWithSubject.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

    // Fetch the attendance percentage when the card is composed or the subject ID changes
    LaunchedEffect(subject.id) {
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(subject.color)).copy(alpha = 0.3f))
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
            // -- CHANGE HERE: Display the current percentage once loaded --
            percentage?.let {
                val percentageColor = if (it >= subject.targetAttendance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
