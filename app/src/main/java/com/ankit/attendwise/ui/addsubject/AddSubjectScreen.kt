package com.ankit.attendwise.ui.addsubject

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.attendwise.data.ClassSchedule
import com.ankit.attendwise.data.Subject
import com.ankit.attendwise.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

data class UiClassSchedule(
    val schedule: ClassSchedule,
    val localId: UUID = UUID.randomUUID()
)

val predefinedColors = listOf(
    "#81C784", "#FF8A65", "#4FC3F7", "#F06292", "#FFD54F",
    "#9575CD", "#4DB6AC", "#A1887F", "#7986CB"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    navController: NavController,
    subjectId: String = "",
    appViewModel: AppViewModel
) {
    var subjectName by remember { mutableStateOf("") }
    var subjectColor by remember { mutableStateOf(predefinedColors.first()) }
    var attendanceTarget by remember { mutableStateOf(75) }
    var schedules by remember { mutableStateOf<List<UiClassSchedule>>(emptyList()) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    var pastPresent by remember { mutableStateOf("0") }
    var pastAbsent by remember { mutableStateOf("0") }
    
    val haptic = LocalHapticFeedback.current

    val isEditMode = subjectId.isNotEmpty()
    val hasInvalidSchedules = schedules.any { 
        it.schedule.startHour > it.schedule.endHour || 
        (it.schedule.startHour == it.schedule.endHour && it.schedule.startMinute >= it.schedule.endMinute)
    }

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

    var scheduleToDelete by remember { mutableStateOf<UiClassSchedule?>(null) }

    if (scheduleToDelete != null) {
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Remove Schedule") },
            text = { Text("Are you sure you want to remove this class schedule?") },
            confirmButton = {
                Button(
                    onClick = {
                        schedules = schedules.filter { it.localId != scheduleToDelete!!.localId }
                        scheduleToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton({ scheduleToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Subject" else "Add Subject") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newSubject = Subject(
                                id = if (isEditMode) subjectId else "",
                                name = subjectName,
                                color = subjectColor,
                                targetAttendance = attendanceTarget
                            )
                            appViewModel.addOrUpdateSubject(
                                newSubject,
                                schedules.map { it.schedule },
                                pastPresent.toIntOrNull() ?: 0,
                                pastAbsent.toIntOrNull() ?: 0
                            )
                            navController.popBackStack()
                        },
                        enabled = subjectName.isNotBlank() && schedules.isNotEmpty() && !hasInvalidSchedules
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
            if (hasInvalidSchedules) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Some schedules have end times before or equal to start times. Please fix them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Book, null) }
                )
            }
            item { ColorPicker(selectedColor = subjectColor, onColorSelected = { subjectColor = it }) }
            item { AttendanceTargetSlider(attendanceTarget, onTargetChange = { attendanceTarget = it }) }

            if (!isEditMode) {
                item {
                    PastAttendanceInput(
                        presentCount = pastPresent,
                        absentCount = pastAbsent,
                        onPresentChange = { pastPresent = it },
                        onAbsentChange = { pastAbsent = it }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Class Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = { showAddScheduleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            if (schedules.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No schedules added yet. Click 'Add' to create one.",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(schedules, key = { it.localId }) { uiSchedule ->
                    ScheduleCard(uiSchedule.schedule) {
                        scheduleToDelete = uiSchedule
                    }
                }
            }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onAddSchedule = { newSchedule -> schedules = schedules + UiClassSchedule(newSchedule) }
        )
    }
}

@Composable
private fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subject Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(predefinedColors) { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .clickable { onColorSelected(color) }
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceTargetSlider(target: Int, onTargetChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Attendance Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$target%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
private fun PastAttendanceInput(
    presentCount: String,
    absentCount: String,
    onPresentChange: (String) -> Unit,
    onAbsentChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Past Attendance (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Add previous classes if you're already tracking this subject elsewhere.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = presentCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) onPresentChange(it) },
                    label = { Text("Attended") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = absentCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) onAbsentChange(it) },
                    label = { Text("Missed") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: ClassSchedule, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(getDayName(schedule.dayOfWeek), fontWeight = FontWeight.Bold)
                Text("${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete schedule", tint = MaterialTheme.colorScheme.error)
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

    val startTimePickerDialog = TimePickerDialog(context, { _, h, m -> startHour = h; startMinute = m }, startHour, startMinute, false)
    val endTimePickerDialog = TimePickerDialog(context, { _, h, m -> endHour = h; endMinute = m }, endHour, endMinute, false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Class Schedule")
            }
        },
        text = {
            Column {
                Divider(modifier = Modifier.padding(bottom = 16.dp))
                DaySelector(selectedDay) { selectedDay = it }
                Spacer(Modifier.height(16.dp))
                TimeSelector("Start Time", startHour, startMinute) { startTimePickerDialog.show() }
                Spacer(Modifier.height(8.dp))
                TimeSelector("End Time", endHour, endMinute) { endTimePickerDialog.show() }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddSchedule(ClassSchedule(id = "", subjectId = "", dayOfWeek = selectedDay, startHour = startHour, startMinute = startMinute, endHour = endHour, endMinute = endMinute))
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
        Text("Day of the week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Text(label, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClick) {
            Text(formatTime(hour, minute))
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
