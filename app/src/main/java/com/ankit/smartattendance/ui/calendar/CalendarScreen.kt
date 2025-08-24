package com.ankit.smartattendance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.RecordType
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
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(appViewModel: AppViewModel) {
    val allRecords by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())
    val showHolidayDialog by appViewModel.showHolidayDialog.collectAsState()

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    if (showHolidayDialog != null) {
        HolidayConfirmationDialog(
            onConfirm = { appViewModel.onHolidayToggleConfirmed() },
            onDismiss = { appViewModel.onHolidayToggleDismissed() }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Holiday Calendar") }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            val daysOfWeek = remember {
                val days = DayOfWeek.values()
                val first = firstDayOfWeekFromLocale()
                days.slice(days.indexOf(first)..days.lastIndex) + days.slice(0 until days.indexOf(first))
            }
            DaysOfWeekTitle(daysOfWeek = daysOfWeek)
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(day, allRecords) { date ->
                        appViewModel.onHolidayToggleRequested(date)
                    }
                }
            )
        }
    }
}

@Composable
private fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Day(day: CalendarDay, allRecords: List<AttendanceRecord>, onDayClick: (LocalDate) -> Unit) {
    val recordsForDay = remember(day, allRecords) {
        allRecords.filter { it.date != 0L && LocalDate.ofEpochDay(it.date) == day.date }
    }

    val isHoliday = recordsForDay.any { it.type == RecordType.HOLIDAY }
    val wasPresent = recordsForDay.any { it.type != RecordType.HOLIDAY && it.isPresent }
    val wasAbsent = recordsForDay.any { it.type != RecordType.HOLIDAY && !it.isPresent }

    val dayBackgroundColor = when {
        isHoliday -> HolidayYellow.copy(alpha = 0.5f)
        wasPresent && wasAbsent -> Color.Gray.copy(alpha = 0.4f) // Mixed attendance
        wasPresent -> SuccessGreen.copy(alpha = 0.4f)
        wasAbsent -> ErrorRed.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color = dayBackgroundColor)
            .clickable(
                enabled = day.date <= LocalDate.now(),
                onClick = { onDayClick(day.date) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.date > LocalDate.now()) Color.Gray.copy(alpha = 0.6f) else LocalContentColor.current
        )
    }
}

@Composable
fun HolidayConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Holiday") },
        text = { Text("This day has existing attendance records. Marking it as a holiday will remove them. Are you sure?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
