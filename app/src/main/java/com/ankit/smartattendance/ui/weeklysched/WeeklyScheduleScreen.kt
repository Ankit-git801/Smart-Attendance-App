package com.ankit.smartattendance.ui.weeklysched

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(navController: NavController, appViewModel: AppViewModel) {
    val weeklySchedule by appViewModel.getWeeklySchedule().collectAsState(initial = emptyMap())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (weeklySchedule.isEmpty()) {
            EmptyScheduleState(navController, Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val daysOfWeek = (1..7).toList() // Sunday to Saturday
                daysOfWeek.forEach { dayOfWeek ->
                    val dayName = getDayName(dayOfWeek)
                    val schedulesForDay = weeklySchedule[dayOfWeek] ?: emptyList()

                    if (schedulesForDay.isNotEmpty()) {
                        item {
                            Text(
                                text = dayName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(schedulesForDay, key = { it.schedule.id }) { schedule ->
                            ScheduleItem(schedule = schedule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(schedule: ScheduleWithSubject) {
    val subjectColor = Color(android.graphics.Color.parseColor(schedule.subject.color))
    val startTime = formatTime(schedule.schedule.startHour, schedule.schedule.startMinute)
    val endTime = formatTime(schedule.schedule.endHour, schedule.schedule.endMinute)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = schedule.subject.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$startTime - $endTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleState(navController: NavController, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = "No Schedule",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = "No Classes Scheduled",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your weekly schedule is empty. Add a subject and its class timings to see them here.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("add_subject") }) {
                Text("Add a Subject")
            }
        }
    }
}

private fun getDayName(dayOfWeek: Int): String {
    // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ...
    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
    }
    return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}
