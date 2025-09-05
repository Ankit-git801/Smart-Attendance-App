package com.ankit.smartattendance.ui.subjectdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch

sealed class AttendanceAction {
    object Present : AttendanceAction()
    object Absent : AttendanceAction()
    object ExtraClass : AttendanceAction()
    object ClearRegular : AttendanceAction()
    object ClearExtra : AttendanceAction()
    object ClearAll : AttendanceAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subjectId: Long, navController: NavController, appViewModel: AppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var subject by remember { mutableStateOf<Subject?>(null) }
    var stats by remember { mutableStateOf(Pair(0.0, 0)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showMarkAttendanceDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val attendanceRecords by appViewModel.getAttendanceRecordsForSubject(subjectId).collectAsState(initial = emptyList())

    LaunchedEffect(subjectId, attendanceRecords) {
        coroutineScope.launch {
            subject = appViewModel.getSubjectById(subjectId)
            val total = appViewModel.getTotalClassesForSubject(subjectId)
            val present = appViewModel.getPresentClassesForSubject(subjectId)
            val percentage = if (total > 0) (present.toDouble() / total) * 100 else 0.0
            stats = Pair(percentage, total)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Subject") },
            text = { Text("Are you sure you want to delete '${subject?.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    subject?.let { appViewModel.deleteSubject(it) }
                    navController.popBackStack()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton({ showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showManualAddDialog) {
        ManualAddAttendanceDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { present, absent ->
                appViewModel.addManualAttendance(subjectId, present, absent)
                showManualAddDialog = false
            }
        )
    }

    if (showMarkAttendanceDialog && selectedDate != null) {
        val recordsForSelectedDate = remember(selectedDate, attendanceRecords) {
            attendanceRecords.filter { it.date == selectedDate!!.toEpochDay() }
        }
        MarkAttendanceDialog(
            date = selectedDate!!,
            recordsForDay = recordsForSelectedDate,
            onDismiss = { showMarkAttendanceDialog = false },
            onConfirm = { action ->
                selectedDate?.let { date ->
                    when (action) {
                        AttendanceAction.Present -> appViewModel.markAttendanceForDate(subjectId, date, true)
                        AttendanceAction.Absent -> appViewModel.markAttendanceForDate(subjectId, date, false)
                        AttendanceAction.ExtraClass -> appViewModel.markExtraClassAttendanceForDate(subjectId, date)
                        AttendanceAction.ClearRegular -> appViewModel.clearRegularAttendanceForDate(subjectId, date)
                        AttendanceAction.ClearExtra -> appViewModel.clearExtraClassAttendanceForDate(subjectId, date)
                        AttendanceAction.ClearAll -> appViewModel.markAttendanceForDate(subjectId, date, null)
                    }
                }
                showMarkAttendanceDialog = false
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(subject?.name ?: "Details") },
            navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = {
                IconButton({ showManualAddDialog = true }) { Icon(Icons.Default.Add, "Add Manual Attendance") }
                IconButton({ navController.navigate("edit_subject/$subjectId") }) { Icon(Icons.Default.Edit, "Edit") }
                IconButton({ showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        )
    }) { paddingValues ->
        LazyColumn(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                subject?.let {
                    AttendanceProgressCard(
                        subjectName = it.name,
                        percentage = stats.first,
                        target = it.targetAttendance,
                        color = Color(android.graphics.Color.parseColor(it.color))
                    )
                }
            }
            item { Text("Attendance Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                AttendanceCalendar(
                    records = attendanceRecords,
                    onDayClick = { date ->
                        selectedDate = date
                        showMarkAttendanceDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun MarkAttendanceDialog(
    date: LocalDate,
    recordsForDay: List<AttendanceRecord>,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceAction) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    val hasRegularClass = recordsForDay.any { it.scheduleId != null }
    val hasExtraClass = recordsForDay.any { it.scheduleId == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Attendance") },
        text = {
            Column {
                Text(date.format(formatter), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                Text("Regular Class", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(bottom = 8.dp))
                AttendanceActionRow(Icons.Default.CheckCircle, "Mark Present", onClick = { onConfirm(AttendanceAction.Present) })
                AttendanceActionRow(Icons.Default.Cancel, "Mark Absent", onClick = { onConfirm(AttendanceAction.Absent) })
                if (hasRegularClass) {
                    AttendanceActionRow(Icons.Default.DeleteOutline, "Clear Regular", onClick = { onConfirm(AttendanceAction.ClearRegular) }, isDestructive = true)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Extra Class", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(bottom = 8.dp))
                AttendanceActionRow(Icons.Default.AddCircle, "Add Extra Class (Present)", onClick = { onConfirm(AttendanceAction.ExtraClass) })
                if (hasExtraClass) {
                    AttendanceActionRow(Icons.Default.DeleteOutline, "Clear Extra Class", onClick = { onConfirm(AttendanceAction.ClearExtra) }, isDestructive = true)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AttendanceActionRow(icon: ImageVector, text: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(text, color = if (isDestructive) MaterialTheme.colorScheme.error else LocalContentColor.current)
    }
}

@Composable
private fun ManualAddAttendanceDialog(onDismiss: () -> Unit, onConfirm: (present: Int, absent: Int) -> Unit) {
    var presentCount by remember { mutableStateOf("") }
    var absentCount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Past Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the number of classes held before you started using the app.")
                OutlinedTextField(value = presentCount, onValueChange = { presentCount = it.filter { c -> c.isDigit() } }, label = { Text("Classes Attended") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = absentCount, onValueChange = { absentCount = it.filter { c -> c.isDigit() } }, label = { Text("Classes Missed") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button({ onConfirm(presentCount.toIntOrNull() ?: 0, absentCount.toIntOrNull() ?: 0) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AttendanceProgressCard(subjectName: String, percentage: Double, target: Int, color: Color) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedCircularProgress(percentage = percentage.toFloat(), color = color)
            Spacer(Modifier.width(24.dp))
            Column {
                Text(subjectName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Target: $target%", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
@Composable
private fun AttendanceCalendar(records: List<AttendanceRecord>, onDayClick: (LocalDate) -> Unit) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)
    val daysOfWeek = remember {
        val days = DayOfWeek.values()
        if (firstDayOfWeek == DayOfWeek.MONDAY) days else (days.sliceArray(1..6) + days[0])
    }

    Column {
        DaysOfWeekTitle(daysOfWeek = daysOfWeek)
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val recordsForDay = remember(day.date, records) {
                    records.filter { record -> record.date == day.date.toEpochDay() }
                }
                Day(day, recordsForDay, onClick = { onDayClick(it.date) })
            }
        )
    }
}

@Composable
private fun Day(day: CalendarDay, records: List<AttendanceRecord>, onClick: (CalendarDay) -> Unit) {
    val isToday = day.date == LocalDate.now()

    val wasPresent = records.any { it.isPresent }
    val wasAbsent = records.any { !it.isPresent }
    val hasExtraClass = records.any { it.scheduleId == null }

    val dayBackgroundColor = when {
        records.isEmpty() -> Color.Transparent
        wasPresent && wasAbsent -> Color.Gray.copy(alpha = 0.4f)
        wasPresent -> SuccessGreen.copy(alpha = 0.4f)
        wasAbsent -> ErrorRed.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    // **THE FIX IS HERE**: The parent Box now handles the click, and a separate inner Box is used
    // for the main date display, allowing the 'E' indicator to be positioned outside of it.
    Box(
        Modifier
            .aspectRatio(1f)
            .clickable(enabled = day.date <= LocalDate.now()) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f) // Make the date circle slightly smaller
                .clip(CircleShape)
                .background(color = dayBackgroundColor)
                .border(
                    width = if (isToday) 2.dp else 0.dp,
                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (isToday) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }

        if (hasExtraClass) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // No padding needed, as it's now positioned relative to the outer box
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "E",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekTitle(daysOfWeek: Array<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Composable
fun AnimatedCircularProgress(percentage: Float, color: Color, radius: Dp = 50.dp, strokeWidth: Dp = 8.dp) {
    val animatedPercentage by animateFloatAsState(targetValue = percentage, animationSpec = tween(1000))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(color = color.copy(alpha = 0.3f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
            drawArc(color = color, startAngle = -90f, sweepAngle = (animatedPercentage / 100) * 360f, useCenter = false, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
        }
        Text(text = "${animatedPercentage.toInt()}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
