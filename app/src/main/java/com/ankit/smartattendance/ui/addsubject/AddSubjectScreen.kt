package com.ankit.smartattendance.ui.addsubject

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

// A list of predefined colors for the user to choose from.
val subjectColors = listOf(
    "#81C784", // Green
    "#64B5F6", // Blue
    "#FFB74D", // Orange
    "#E57373", // Red
    "#BA68C8", // Purple
    "#4DB6AC", // Teal
    "#FFF176", // Yellow
    "#90A4AE"  // Blue Grey
)

data class UiClassSchedule(
    val schedule: ClassSchedule,
    val localId: UUID = UUID.randomUUID()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    navController: NavController,
    subjectId: Long = 0L,
    appViewModel: AppViewModel
) {
    var subjectName by remember { mutableStateOf("") }
    var subjectColor by remember { mutableStateOf(subjectColors.first()) }
    var attendanceTarget by remember { mutableStateOf(75) }
    var schedules by remember { mutableStateOf<List<UiClassSchedule>>(emptyList()) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    val isEditMode = subjectId != 0L

    LaunchedEffect(subjectId) {
        if (isEditMode) {
            appViewModel.getSubjectById(subjectId)?.let { subject ->
                subjectName = subject.name
                subjectColor = subject.color
                attendanceTarget = subject.targetAttendance
            }
            schedules = appViewModel.getSchedulesForSubject(subjectId).map { UiClassSchedule(it) }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onAddSchedule = { newSchedule ->
                schedules = schedules + UiClassSchedule(newSchedule)
                showAddScheduleDialog = false // Dismiss dialog after adding
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Subject" else "Add New Subject") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val newSubject = Subject(
                                id = if (isEditMode) subjectId else 0,
                                name = subjectName.trim(),
                                color = subjectColor,
                                targetAttendance = attendanceTarget
                            )
                            appViewModel.addOrUpdateSubject(newSubject, schedules.map { it.schedule })
                            navController.popBackStack()
                        },
                        enabled = subjectName.isNotBlank() && schedules.isNotEmpty()
                    ) {
                        Text(if (isEditMode) "UPDATE" else "SAVE")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section for Subject Name and Color
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = { subjectName = it },
                            label = { Text("Subject Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        ColorPicker(
                            selectedColor = subjectColor,
                            onColorSelected = { subjectColor = it }
                        )
                    }
                }
            }

            item { AttendanceTargetSlider(target = attendanceTarget, onTargetChange = { attendanceTarget = it }) }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Class Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { showAddScheduleDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    if (schedules.isEmpty()) {
                        Text(
                            "A subject must have at least one weekly schedule.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(schedules, key = { it.localId }) { uiSchedule ->
                ScheduleCard(uiSchedule.schedule) {
                    schedules = schedules.filter { it.localId != uiSchedule.localId }
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    Column {
        Text("Subject Color", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subjectColors) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color)))
                        .clickable { onColorSelected(color) }
                ) {
                    if (selectedColor == color) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceTargetSlider(target: Int, onTargetChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text("Attendance Target", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "$target%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = target.toFloat(),
                onValueChange = { onTargetChange(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9
            )
        }
    }
}

@Composable
private fun ScheduleCard(schedule: ClassSchedule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(getDayName(schedule.dayOfWeek), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete schedule",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddScheduleDialog(onDismiss: () -> Unit, onAddSchedule: (ClassSchedule) -> Unit) {
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    val calendar = Calendar.getInstance()
    var startHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    calendar.add(Calendar.HOUR_OF_DAY, 1)
    var endHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    val context = LocalContext.current
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    if (showStartTimePicker) {
        TimePickerDialog(context, { _, h, m -> startHour = h; startMinute = m; showStartTimePicker = false }, startHour, startMinute, false)
            .show()
    }

    if (showEndTimePicker) {
        TimePickerDialog(context, { _, h, m -> endHour = h; endMinute = m; showEndTimePicker = false }, endHour, endMinute, false)
            .show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Class Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DaySelector(selectedDay) { selectedDay = it }
                TimeSelector("Start Time", startHour, startMinute) { showStartTimePicker = true }
                TimeSelector("End Time", endHour, endMinute) { showEndTimePicker = true }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddSchedule(ClassSchedule(0, 0, selectedDay, startHour, startMinute, endHour, endMinute))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf(
        "Mon" to Calendar.MONDAY, "Tue" to Calendar.TUESDAY, "Wed" to Calendar.WEDNESDAY,
        "Thu" to Calendar.THURSDAY, "Fri" to Calendar.FRIDAY, "Sat" to Calendar.SATURDAY, "Sun" to Calendar.SUNDAY
    )
    Column {
        Text("Day of the week", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(days) { (dayName, dayConstant) ->
                FilterChip(
                    selected = selectedDay == dayConstant,
                    onClick = { onDaySelected(dayConstant) },
                    label = { Text(dayName) }
                )
            }
        }
    }
}

@Composable
private fun TimeSelector(label: String, hour: Int, minute: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        OutlinedButton(onClick = onClick) {
            Text(formatTime(hour, minute), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun getDayName(dayOfWeek: Int): String {
    val calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }
    return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}
