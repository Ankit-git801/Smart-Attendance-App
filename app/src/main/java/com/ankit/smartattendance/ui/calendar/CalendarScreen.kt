package com.ankit.smartattendance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
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
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.RecordType
import com.ankit.smartattendance.models.AttendanceRecordWithSubject
import com.ankit.smartattendance.ui.theme.HolidayYellow
import com.ankit.smartattendance.viewmodel.AppViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController, appViewModel: AppViewModel) {
    val allRecords by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val holidayConfirmationDate by appViewModel.showHolidayDialog.collectAsState()

    if (holidayConfirmationDate != null) {
        HolidayConfirmationDialog(
            onConfirm = { appViewModel.onHolidayToggleConfirmed() },
            onDismiss = { appViewModel.onHolidayToggleDismissed() }
        )
    }

    selectedDate?.let { date ->
        val recordsForDay by appViewModel.getRecordsForDate(date).collectAsState(initial = emptyList())
        val isHoliday = allRecords.any { it.date == date.toEpochDay() && it.type == RecordType.HOLIDAY }

        DayDetailDialog(
            date = date,
            recordsForDay = recordsForDay,
            isHoliday = isHoliday,
            onDismiss = { selectedDate = null },
            onSubjectClick = { subjectId ->
                selectedDate = null
                navController.navigate("subject_detail/$subjectId")
            },
            onHolidayToggle = { appViewModel.onHolidayToggleRequested(date) }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calendar") }) }) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            AttendanceCalendar(
                allRecords = allRecords,
                onDayClick = { selectedDate = it }
            )
        }
    }
}

@Composable
private fun DayDetailDialog(
    date: LocalDate,
    recordsForDay: List<AttendanceRecordWithSubject>,
    isHoliday: Boolean,
    onDismiss: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    onHolidayToggle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))) },
        text = {
            Column {
                if (isHoliday) {
                    Text("This day is marked as a holiday. All attendance for this day is ignored.")
                } else if (recordsForDay.isEmpty()) {
                    Text("No classes were attended or missed on this day.")
                } else {
                    LazyColumn {
                        items(recordsForDay, key = { "${it.attendanceRecord.id}-${it.attendanceRecord.date}" }) { recordItem ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(recordItem.subjectColor ?: "#808080"))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSubjectClick(recordItem.attendanceRecord.subjectId) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Circle, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(recordItem.subjectName ?: "Unknown Subject", modifier = Modifier.weight(1f))
                                Text(
                                    if (recordItem.attendanceRecord.isPresent) "Present" else "Absent",
                                    color = if (recordItem.attendanceRecord.isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onHolidayToggle()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHoliday) MaterialTheme.colorScheme.secondaryContainer else HolidayYellow
                )
            ) {
                Text(if (isHoliday) "Unmark as Holiday" else "Mark as Holiday")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HolidayConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Holiday?") },
        text = { Text("Marking this day as a holiday will delete all existing attendance records for this day and prevent future notifications. Are you sure?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = HolidayYellow)
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

    Column {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        Text(
            text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(day, allRecords, onDayClick)
            }
        )
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    allRecords: List<AttendanceRecord>,
    onDayClick: (LocalDate) -> Unit
) {
    val recordsForDay = remember(day.date, allRecords) {
        allRecords.filter { it.date == day.date.toEpochDay() }
    }
    val isToday = day.date == LocalDate.now()

    val isHoliday = recordsForDay.any { it.type == RecordType.HOLIDAY }
    val hasAttendance = recordsForDay.any { it.type != RecordType.HOLIDAY }

    val dayBackgroundColor = when {
        isHoliday -> HolidayYellow.copy(alpha = 0.5f)
        hasAttendance -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
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
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                else -> LocalContentColor.current
            },
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
