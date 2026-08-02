@file:OptIn(ExperimentalMaterial3Api::class)

package com.ankit.attendwise.ui.calendar

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ankit.attendwise.data.AttendanceRecord
import com.ankit.attendwise.data.RecordType
import com.ankit.attendwise.models.AttendanceRecordWithSubject
import com.ankit.attendwise.ui.theme.ErrorRed
import com.ankit.attendwise.ui.theme.HolidayYellow
import com.ankit.attendwise.ui.theme.SuccessGreen
import com.ankit.attendwise.viewmodel.AppViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(navController: NavController, appViewModel: AppViewModel) {
    val allRecords by appViewModel.allAttendanceRecords.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showHolidayConfirmation by remember { mutableStateOf(false) }

    val recordsForSelectedDate by if (selectedDate != null) {
        appViewModel.getRecordsForDate(selectedDate!!).collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList<AttendanceRecordWithSubject>()) }
    }

    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this attendance record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val recordId = showDeleteConfirmation!!
                        val subjectId = recordsForSelectedDate.find { it.attendanceRecord.id == recordId }?.attendanceRecord?.subjectId ?: ""
                        appViewModel.deleteAttendanceRecordById(recordId, subjectId)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = { 
                TextButton(
                    onClick = { showDeleteConfirmation = null },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") } 
            }
        )
    }

    if (showHolidayConfirmation && selectedDate != null) {
        HolidayConfirmationDialog(
            onConfirm = {
                appViewModel.onHolidayToggleConfirmed()
                showHolidayConfirmation = false
                showDialog = false
            },
            onDismiss = { showHolidayConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Attendance Calendar") })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AttendanceCalendar(
                allRecords = allRecords,
                onDayClick = { date ->
                    selectedDate = date
                    showDialog = true
                }
            )

            if (showDialog && selectedDate != null) {
                val holiday = allRecords.find { it.date == selectedDate!!.toEpochDay() && it.type == RecordType.HOLIDAY }
                val isCurrentlyHoliday = holiday != null

                DayDetailDialog(
                    date = selectedDate!!,
                    records = recordsForSelectedDate,
                    isHoliday = isCurrentlyHoliday,
                    onDismiss = { showDialog = false },
                    onDeleteRecord = { recordId ->
                        showDeleteConfirmation = recordId
                    },
                    onToggleHoliday = {
                        if (isCurrentlyHoliday) {
                            appViewModel.onHolidayToggleRequested(selectedDate!!)
                            showDialog = false
                        } else {
                            appViewModel.onHolidayToggleRequested(selectedDate!!) // This sets the _showHolidayDialog value
                            showHolidayConfirmation = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DayDetailDialog(
    date: LocalDate,
    records: List<AttendanceRecordWithSubject>,
    isHoliday: Boolean,
    onDismiss: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onToggleHoliday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isHoliday) {
                    Text(
                        "Public Holiday / No Classes",
                        color = HolidayYellow,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (records.isEmpty() && !isHoliday) {
                    Text("No records for this day")
                }

                records.forEach { recordWithSubject ->
                    val record = recordWithSubject.attendanceRecord
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recordWithSubject.subjectName ?: "Unknown",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when {
                                    record.type == RecordType.CANCELLED -> "Cancelled"
                                    record.isPresent -> "Present"
                                    else -> "Absent"
                                }
                                val statusColor = when {
                                    record.type == RecordType.CANCELLED -> MaterialTheme.colorScheme.outline
                                    record.isPresent -> SuccessGreen
                                    else -> ErrorRed
                                }
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (record.note.isNotEmpty()) {
                                    Text(
                                        text = record.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteRecord(record.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onToggleHoliday,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isHoliday) "Remove Holiday" else "Mark as Holiday")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HolidayConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Holiday?") },
        text = { Text("This will remove all attendance records for this date. Do you want to proceed?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
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

    val recordsByDate = remember(allRecords) {
        allRecords.groupBy { it.date }
    }

    val coroutineScope = rememberCoroutineScope()

    Column {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
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
                text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                val dayRecords = recordsByDate[day.date.toEpochDay()] ?: emptyList()
                Day(day.date, dayRecords, onDayClick)
            }
        )
    }
}

@Composable
fun Day(
    date: LocalDate,
    records: List<AttendanceRecord>,
    onClick: (LocalDate) -> Unit
) {
    val isHoliday = records.any { it.type == RecordType.HOLIDAY }
    val hasRecords = records.any { it.type != RecordType.HOLIDAY }
    val isToday = date == LocalDate.now()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isHoliday -> HolidayYellow.copy(alpha = 0.2f)
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable { onClick(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
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
