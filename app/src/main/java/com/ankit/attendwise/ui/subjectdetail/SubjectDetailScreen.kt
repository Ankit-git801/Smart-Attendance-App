@file:OptIn(ExperimentalMaterial3Api::class)

package com.ankit.attendwise.ui.subjectdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ankit.attendwise.data.AttendanceRecord
import com.ankit.attendwise.data.BunkAnalysis
import com.ankit.attendwise.data.RecordType
import com.ankit.attendwise.data.Subject
import com.ankit.attendwise.models.SubjectWithAttendance
import com.ankit.attendwise.ui.theme.ErrorRed
import com.ankit.attendwise.ui.theme.HolidayYellow
import com.ankit.attendwise.ui.theme.PoppinsFamily
import com.ankit.attendwise.ui.theme.SuccessGreen
import com.ankit.attendwise.viewmodel.AppViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subjectId: String, navController: NavController, appViewModel: AppViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showMarkAttendanceDialog by remember { mutableStateOf<LocalDate?>(null) }
    val haptic = LocalHapticFeedback.current

    val subjectsWithAttendance by appViewModel.subjectsWithAttendance.collectAsStateWithLifecycle()
    val bunkAnalysisMap by appViewModel.bunkAnalysisMap.collectAsStateWithLifecycle()
    val subjectWithAttendance by remember(subjectsWithAttendance, subjectId) {
        derivedStateOf { subjectsWithAttendance.find { it.subject.id == subjectId } }
    }

    val bunkAnalysis = remember(bunkAnalysisMap, subjectId) { bunkAnalysisMap[subjectId] }

    val attendanceRecords by appViewModel.getAttendanceRecordsForSubject(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())

    var recordToDelete by remember { mutableStateOf<String?>(null) }
    var clearAllDateRecords by remember { mutableStateOf<LocalDate?>(null) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this attendance record?") },
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.deleteAttendanceRecordById(recordToDelete!!, subjectId)
                        recordToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { recordToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") } 
            }
        )
    }

    if (clearAllDateRecords != null) {
        AlertDialog(
            onDismissRequest = { clearAllDateRecords = null },
            title = { Text("Clear Attendance") },
            text = { Text("Are you sure you want to clear all attendance records for this date?") },
            confirmButton = {
                Button(
                    onClick = {
                        appViewModel.deleteAttendanceRecordForDate(subjectId, clearAllDateRecords!!)
                        clearAllDateRecords = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Clear") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { clearAllDateRecords = null },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") } 
            }
        )
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
                appViewModel.addPastRecords(subjectId, present, absent)
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
            onConfirm = { isPresent ->
                appViewModel.updateAttendanceRecord(subjectId, date, isPresent)
            },
            onDeleteMain = { clearAllDateRecords = date },
            onDeleteRecord = { recordId -> recordToDelete = recordId },
            onAddExtra = { isPresent -> appViewModel.addExtraClasses(subjectId, date, isPresent, 1) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectWithAttendance?.subject?.name ?: "Subject Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showManualAddDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add Past Records")
                    }
                    IconButton(onClick = {
                        navController.navigate("edit_subject/${subjectId}")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (subjectWithAttendance == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AttendanceProgressCard(subjectWithAttendance!!)
                AttendanceStatsCard(subjectWithAttendance!!)
                if (bunkAnalysis != null) {
                    BunkAnalysisCard(bunkAnalysis!!, subjectWithAttendance!!.subject)
                }

                Text(
                    "Attendance History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                AttendanceCalendar(attendanceRecords) { date ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMarkAttendanceDialog = date
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(subjectName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Subject") },
        text = { Text("Are you sure you want to delete '$subjectName'? This will also delete all its attendance records and schedules.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Delete") }
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") } 
        }
    )
}

@Composable
fun ManualAddAttendanceDialog(onDismiss: () -> Unit, onConfirm: (present: Int, absent: Int) -> Unit) {
    var presentCount by remember { mutableStateOf("0") }
    var absentCount by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Past Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter the number of classes you've already attended or missed.")
                OutlinedTextField(
                    value = presentCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) presentCount = it },
                    label = { Text("Total Attended (Present)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = absentCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) absentCount = it },
                    label = { Text("Total Missed (Absent)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(presentCount.toIntOrNull() ?: 0, absentCount.toIntOrNull() ?: 0) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Add Records") }
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") } 
        }
    )
}

@Composable
fun MarkAttendanceDialog(
    date: LocalDate,
    recordsForDay: List<AttendanceRecord>,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    onDeleteMain: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onAddExtra: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.toString(), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (recordsForDay.isEmpty()) {
                    Text("No attendance marked for this day.")
                } else {
                    recordsForDay.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val status = when {
                                    record.type == RecordType.CANCELLED -> "Cancelled"
                                    record.isPresent -> "Present"
                                    else -> "Absent"
                                }
                                val color = when {
                                    record.type == RecordType.CANCELLED -> MaterialTheme.colorScheme.outline
                                    record.isPresent -> SuccessGreen
                                    else -> ErrorRed
                                }
                                Text(status, color = color, fontWeight = FontWeight.Bold)
                                if (record.note.isNotEmpty()) {
                                    Text(record.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { onDeleteRecord(record.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Quick Actions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                AttendanceActionRow(Icons.Default.CheckCircle, "Mark Present", { onConfirm(true); onDismiss() }, true)
                AttendanceActionRow(Icons.Default.Cancel, "Mark Absent", { onConfirm(false); onDismiss() }, false)
                AttendanceActionRow(Icons.Default.AddCircle, "Add Extra Class (Present)", { onAddExtra(true); onDismiss() }, true)
                AttendanceActionRow(Icons.Default.RemoveCircle, "Add Extra Class (Absent)", { onAddExtra(false); onDismiss() }, false)
            }
        },
        confirmButton = {
            Button(
                onClick = onDeleteMain,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Clear Day") }
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Close") } 
        }
    )
}

@Composable
fun AttendanceActionRow(icon: ImageVector, label: String, onClick: () -> Unit, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isPositive) SuccessGreen else ErrorRed)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AttendanceProgressCard(subjectWithAttendance: SubjectWithAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedCircularProgress(
                progress = subjectWithAttendance.percentage.toFloat() / 100f,
                color = Color(android.graphics.Color.parseColor(subjectWithAttendance.subject.color)),
                size = 100.dp,
                strokeWidth = 10.dp
            )
            Spacer(Modifier.width(24.dp))
            Column {
                Text(
                    text = subjectWithAttendance.subject.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: ${subjectWithAttendance.subject.targetAttendance}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AttendanceStatsCard(subjectWithAttendance: SubjectWithAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("Total", subjectWithAttendance.totalClasses.toString(), MaterialTheme.colorScheme.primary)
            StatItem("Attended", subjectWithAttendance.presentClasses.toString(), SuccessGreen)
            StatItem("Missed", (subjectWithAttendance.totalClasses - subjectWithAttendance.presentClasses).toString(), ErrorRed)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BunkAnalysisCard(analysis: BunkAnalysis, subject: Subject) {
    val (text, color) = when {
        analysis.classesToBunk > 0 -> {
            "You can bunk ${analysis.classesToBunk} classes while staying above your ${subject.targetAttendance}% target." to SuccessGreen
        }
        analysis.classesToAttend > 0 -> {
            "You must attend the next ${analysis.classesToAttend} classes to reach your ${subject.targetAttendance}% target." to ErrorRed
        }
        else -> {
            "You are exactly on track to meet your ${subject.targetAttendance}% target!" to MaterialTheme.colorScheme.primary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "Bunk analysis", tint = color)
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AnimatedCircularProgress(
    progress: Float,
    color: Color,
    size: Dp,
    strokeWidth: Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                color = color.copy(alpha = 0.1f),
                style = Stroke(width = strokeWidth.toPx())
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AttendanceCalendar(
    allRecords: List<AttendanceRecord>,
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

    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Text(
                text = state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + state.firstVisibleMonth.yearMonth.year,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
        
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val dayRecords = allRecords.filter { it.date == day.date.toEpochDay() }
                Day(day, dayRecords, onDayClick)
            }
        )
    }
}

@Composable
fun Day(
    day: CalendarDay,
    records: List<AttendanceRecord>,
    onClick: (LocalDate) -> Unit
) {
    val isHoliday = records.any { it.type == RecordType.HOLIDAY }
    val hasRecords = records.any { it.type != RecordType.HOLIDAY }
    val isToday = day.date == LocalDate.now()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isHoliday -> HolidayYellow.copy(alpha = 0.2f)
                    isToday && day.position == DayPosition.MonthDate -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && day.position == DayPosition.MonthDate) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday && day.position == DayPosition.MonthDate) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isHoliday -> HolidayYellow
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasRecords) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    records.filter { it.type != RecordType.HOLIDAY }.take(3).forEach { record ->
                        val dotColor = when {
                            record.type == RecordType.CANCELLED -> MaterialTheme.colorScheme.outline
                            record.isPresent -> SuccessGreen
                            else -> ErrorRed
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}
