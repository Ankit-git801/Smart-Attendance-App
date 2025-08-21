package com.ankit.smartattendance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(appViewModel: AppViewModel) {
    val allRecords by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())

    // CORRECTED: Use the simple rememberCalendarState for a StaticCalendar.
    val calendarState = rememberCalendarState(
        initialMonth = YearMonth.now()
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Overall Calendar") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            StaticCalendar(
                calendarState = calendarState,
                dayContent = { dayState ->
                    val recordsForDay = allRecords.filter {
                        LocalDate.ofEpochDay(it.date) == dayState.date
                    }

                    val wasPresent = recordsForDay.any { it.isPresent }
                    val wasAbsent = recordsForDay.any { !it.isPresent }

                    val dayBackgroundColor = when {
                        wasPresent && wasAbsent -> Color.Gray.copy(alpha = 0.4f)
                        wasPresent -> SuccessGreen.copy(alpha = 0.4f)
                        wasAbsent -> ErrorRed.copy(alpha = 0.4f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(dayBackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = dayState.date.dayOfMonth.toString())
                    }
                }
            )
        }
    }
}
