package com.ankit.smartattendance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(appViewModel: AppViewModel) {
    val allRecords by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    Scaffold(topBar = { TopAppBar(title = { Text("Overall Calendar") }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(day, allRecords) { date ->
                        appViewModel.toggleHoliday(date)
                    }
                }
            )
        }
    }
}

@Composable
private fun Day(day: CalendarDay, allRecords: List<AttendanceRecord>, onDayClick: (LocalDate) -> Unit) {
    val recordsForDay = remember(day, allRecords) {
        allRecords.filter { LocalDate.ofEpochDay(it.date) == day.date }
    }

    val isHoliday = recordsForDay.any { it.type == RecordType.HOLIDAY }
    val wasPresent = recordsForDay.any { it.type == RecordType.CLASS && it.isPresent }
    val wasAbsent = recordsForDay.any { it.type == RecordType.CLASS && !it.isPresent }

    val dayBackgroundColor = when {
        isHoliday -> HolidayYellow.copy(alpha = 0.5f)
        wasPresent && wasAbsent -> Color.Gray.copy(alpha = 0.4f)
        wasPresent -> SuccessGreen.copy(alpha = 0.4f)
        wasAbsent -> ErrorRed.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(color = dayBackgroundColor)
            .clickable { onDayClick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.dayOfMonth.toString())
    }
}
