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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.*
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.HolidayYellow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subjectId: Long, navController: NavController, appViewModel: AppViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showMarkAttendanceDialog by remember { mutableStateOf<LocalDate?>(null) }
    var bunkAnalysis by remember { mutableStateOf<BunkAnalysis?>(null) }

    val subjectsWithAttendance by appViewModel.subjectsWithAttendance.collectAsState()
    val subjectWithAttendance by remember(subjectsWithAttendance, subjectId) {
        derivedStateOf { subjectsWithAttendance.find { it.subject.id == subjectId } }
    }

    val attendanceRecords by appViewModel.getAttendanceRecordsForSubject(subjectId).collectAsState(initial = emptyList())

    LaunchedEffect(subjectWithAttendance) {
        subjectWithAttendance?.let {
            bunkAnalysis = appViewModel.calculateBunkAnalysis(it.subject.id)
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            subjectName = subjectWithAttendance?.subject?.name ?: "this subject",
            onConfirm = {
                subjectWithAttendance?.subject?.let { appViewModel.deleteSubject(it) }
                navController.popBackStack()
            },
            onDismiss = { showDeleteDialog = false }
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

    showMarkAttendanceDialog?.let { date ->
        val recordsForSelectedDate = remember(date, attendanceRecords) {
            attendanceRecords.filter { it.date == date.toEpochDay() }
        }
        MarkAttendanceDialog(
            date = date,
            recordsForDay = recordsForSelectedDate,
            onDismiss = { showMarkAttendanceDialog = null },
            onConfirm = { record -> appViewModel.updateAttendanceRecord(record) },
            onDelete = { record -> appViewModel.deleteAttendanceRecord(record) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectWithAttendance?.subject?.name ?: "Details") },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton({ showManualAddDialog = true }) { Icon(Icons.Default.PlaylistAdd, "Add Manual Attendance") }
                    IconButton({ navController.navigate("edit_subject/$subjectId") }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton({ showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            )
        }
    ) { paddingValues ->
        subjectWithAttendance?.let { swa ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { AttendanceProgressCard(subjectWithAttendance = swa) }
                item { AttendanceStatsCard(subjectWithAttendance = swa) }
                item {
                    bunkAnalysis?.let { analysis ->
                        BunkAnalysisCard(analysis = analysis, subject = swa.subject)
                    }
                }
                item {
                    Text(
                        "Attendance History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    AttendanceCalendar(
                        records = attendanceRecords,
                        subjectId = subjectId,
                        onDayClick = { date -> showMarkAttendanceDialog = date }
                    )
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(subjectName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Subject") },
        text = { Text("Are you sure you want to delete '$subjectName'? This action is permanent and cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ManualAddAttendanceDialog(onDismiss: () -> Unit, onConfirm: (present: Int, absent: Int) -> Unit) {
    var presentCount by remember { mutableStateOf("0") }
    var absentCount by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manually Add Records") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add past attendance records that weren't tracked in the app.")
                OutlinedTextField(
                    value = presentCount,
                    onValueChange = { if (it.all(Char::isDigit)) presentCount = it },
                    label = { Text("Present Classes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = absentCount,
                    onValueChange = { if (it.all(Char::isDigit)) absentCount = it },
                    label = { Text("Absent Classes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val present = presentCount.toIntOrNull() ?: 0
                    val absent = absentCount.toIntOrNull() ?: 0
                    if (present > 0 || absent > 0) onConfirm(present, absent)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MarkAttendanceDialog(
    date: LocalDate,
    recordsForDay: List<AttendanceRecord>,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceRecord) -> Unit,
    onDelete: (AttendanceRecord) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    val presentRecord = recordsForDay.find { it.isPresent }
    val absentRecord = recordsForDay.find { !it.isPresent && it.type == RecordType.CLASS }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Attendance") },
        text = {
            Column {
                Text(
                    date.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AttendanceActionRow(Icons.Default.CheckCircle, "Mark as Present", onClick = {
                    val newRecord = presentRecord?.copy(isPresent = true) ?: AttendanceRecord(subjectId = recordsForDay.firstOrNull()?.subjectId ?: 0L, date = date.toEpochDay(), scheduleId = -3L, isPresent = true, type = RecordType.MANUAL, note = "Manually set to Present")
                    onConfirm(newRecord)
                    onDismiss()
                })

                AttendanceActionRow(Icons.Default.Cancel, "Mark as Absent", onClick = {
                    val newRecord = absentRecord?.copy(isPresent = false) ?: AttendanceRecord(subjectId = recordsForDay.firstOrNull()?.subjectId ?: 0L, date = date.toEpochDay(), scheduleId = -3L, isPresent = false, type = RecordType.MANUAL, note = "Manually set to Absent")
                    onConfirm(newRecord)
                    onDismiss()
                })

                recordsForDay.forEach { record ->
                    AttendanceActionRow(Icons.Default.Delete, "Delete '${record.note}'", onClick = {
                        onDelete(record)
                        onDismiss()
                    }, isDestructive = true)
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
private fun AttendanceProgressCard(subjectWithAttendance: SubjectWithAttendance) {
    val subject = subjectWithAttendance.subject
    val percentage = subjectWithAttendance.percentage
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(1000), label = "progress_animation"
    )

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedCircularProgress(
                percentage = animatedPercentage,
                color = Color(android.graphics.Color.parseColor(subject.color)),
                radius = 40.dp,
                strokeWidth = 6.dp
            )
            Spacer(Modifier.width(24.dp))
            Column {
                Text(subject.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Target: ${subject.targetAttendance}%", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AttendanceStatsCard(subjectWithAttendance: SubjectWithAttendance) {
    val total = subjectWithAttendance.totalClasses
    val attended = subjectWithAttendance.presentClasses
    val missed = total - attended

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem("Total", total.toString())
            StatItem("Attended", attended.toString(), SuccessGreen)
            StatItem("Missed", missed.toString(), ErrorRed)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(color = color))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BunkAnalysisCard(analysis: BunkAnalysis, subject: Subject) {
    val message: String
    val contentColor: Color

    when {
        analysis.classesToAttend > 0 -> {
            message = "You must attend the next ${analysis.classesToAttend} classes to reach your ${subject.targetAttendance}% target."
            contentColor = MaterialTheme.colorScheme.error
        }
        analysis.classesToBunk > 0 -> {
            message = "You can afford to bunk the next ${analysis.classesToBunk} classes and still meet your target."
            contentColor = SuccessGreen
        }
        else -> {
            message = "You've met your attendance target. Bunking the next class will put you below the target."
            contentColor = MaterialTheme.colorScheme.primary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = contentColor.copy(alpha = 0.1f),
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "Bunk analysis", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AttendanceCalendar(
    records: List<AttendanceRecord>,
    subjectId: Long,
    onDayClick: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(24) }
    val endMonth = remember { currentMonth.plusMonths(24) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        Text(
            text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(day, records, subjectId, onDayClick)
            }
        )
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    allRecords: List<AttendanceRecord>,
    subjectId: Long,
    onDayClick: (LocalDate) -> Unit
) {
    val recordsForDay = remember(day, allRecords) {
        allRecords.filter { it.subjectId == subjectId && it.date == day.date.toEpochDay() && it.type != RecordType.HOLIDAY }
    }

    val isToday = day.date == LocalDate.now()
    val wasPresent = recordsForDay.any { it.isPresent }
    val wasAbsent = recordsForDay.any { !it.isPresent && it.type != RecordType.CANCELLED }
    val wasCancelled = recordsForDay.any { it.type == RecordType.CANCELLED }

    val dayBackgroundColor = when {
        wasPresent && wasAbsent -> Color.Gray.copy(alpha = 0.4f)
        wasPresent -> SuccessGreen.copy(alpha = 0.4f)
        wasAbsent -> ErrorRed.copy(alpha = 0.4f)
        wasCancelled -> HolidayYellow.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color = dayBackgroundColor)
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onDayClick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (isToday) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
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
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
