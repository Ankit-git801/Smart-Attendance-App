package com.ankit.smartattendance.ui.addsubject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    navController: NavController,
    subjectId: Long = 0L,
    appViewModel: AppViewModel
) {
    var subjectName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var attendanceTarget by remember { mutableStateOf(75) }
    var schedules by remember { mutableStateOf<List<ClassSchedule>>(emptyList()) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    val isEditMode = subjectId != 0L

    LaunchedEffect(subjectId) {
        if (isEditMode) {
            appViewModel.getSubjectById(subjectId)?.let { subject ->
                subjectName = subject.name
                selectedColor = subject.color
                attendanceTarget = subject.targetAttendance
            }
            schedules = appViewModel.getSchedulesForSubject(subjectId)
        }
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
                            val newSubject = Subject(
                                id = if (isEditMode) subjectId else 0,
                                name = subjectName,
                                color = selectedColor,
                                targetAttendance = attendanceTarget
                            )
                            appViewModel.addOrUpdateSubject(newSubject, schedules)
                            navController.popBackStack()
                        },
                        enabled = subjectName.isNotBlank()
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
            item {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
            }
            item { ColorSelector(selectedColor, onColorSelected = { selectedColor = it }) }
            item { AttendanceTargetSlider(attendanceTarget, onTargetChange = { attendanceTarget = it }) }
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(schedules) { schedule ->
                    ScheduleCard(schedule) { schedules = schedules - schedule }
                }
            }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onAddSchedule = { newSchedule -> schedules = schedules + newSchedule }
        )
    }
}

@Composable
private fun ColorSelector(selectedColor: String, onColorSelected: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Subject Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            val colors = listOf(
                "#E57373" to "Red", "#81C784" to "Green", "#64B5F6" to "Blue", "#FFD54F" to "Yellow",
                "#BA68C8" to "Purple", "#FF8A65" to "Orange", "#4DB6AC" to "Teal", "#7986CB" to "Indigo"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(colors) { (colorHex, colorName) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(if (colorHex == selectedColor) 4.dp else 0.dp, CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                                .clickable { onColorSelected(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == selectedColor) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                            }
                        }
                        Text(colorName, style = MaterialTheme.typography.labelSmall)
                    }
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
private fun ScheduleCard(schedule: ClassSchedule, onDelete: (ClassSchedule) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(getDayName(schedule.dayOfWeek), fontWeight = FontWeight.Bold)
                Text("${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}")
            }
            IconButton(onClick = { onDelete(schedule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete schedule", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(onDismiss: () -> Unit, onAddSchedule: (ClassSchedule) -> Unit) {
    var selectedDay by remember { mutableStateOf(Calendar.MONDAY) }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(10) }
    var endMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Class Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DaySelector(selectedDay) { selectedDay = it }
                TimeSelector("Start Time", startHour, startMinute) { h, m -> startHour = h; startMinute = m }
                TimeSelector("End Time", endHour, endMinute) { h, m -> endHour = h; endMinute = m }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddSchedule(ClassSchedule(0, 0, selectedDay, startHour, startMinute, endHour, endMinute))
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Column {
        Text("Day of the week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days.size) { index ->
                val dayOfWeek = if (index < 6) index + 2 else 1
                FilterChip(
                    selected = selectedDay == dayOfWeek,
                    onClick = { onDaySelected(dayOfWeek) },
                    label = { Text(days[index]) }
                )
            }
        }
    }
}

@Composable
private fun TimeSelector(label: String, hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour.toString(),
                onValueChange = { h -> onTimeChange(h.toIntOrNull()?.coerceIn(0, 23) ?: hour, minute) },
                label = { Text("Hour") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Text(":", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = minute.toString(),
                onValueChange = { m -> onTimeChange(hour, m.toIntOrNull()?.coerceIn(0, 59) ?: minute) },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun getDayName(dayOfWeek: Int): String = SimpleDateFormat("EEEE", Locale.getDefault()).format(
    Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }.time
)

private fun formatTime(hour: Int, minute: Int): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(
    Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }.time
)
