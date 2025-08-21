package com.ankit.smartattendance.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    appViewModel: AppViewModel
) {
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsState()
    val allSubjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    val currentDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Padding for the FAB
    ) {
        item {
            Header(currentDate)
        }

        item {
            SectionTitle("Today's Schedule", Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp))
        }

        if (todaysSchedule.isEmpty()) {
            item {
                NoClassesCard()
            }
        } else {
            itemsIndexed(
                items = todaysSchedule,
                key = { _, item -> item.schedule.id }
            ) { index, scheduleWithSubject ->
                ClassCard(
                    scheduleWithSubject = scheduleWithSubject,
                    onMarkAttendance = { schedule, isPresent ->
                        appViewModel.markAttendance(schedule.subject.id, schedule.schedule.id, isPresent)
                    },
                    modifier = Modifier.animateItemPlacement(
                        tween(durationMillis = 300, delayMillis = index * 50)
                    )
                )
            }
        }

        item {
            SectionTitle("Subjects Overview", Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp))
        }

        itemsIndexed(
            items = allSubjects,
            key = { _, item -> item.id }
        ) { index, subject ->
            SubjectOverviewCard(
                subject = subject,
                onClick = { navController.navigate("subject_detail/${subject.id}") },
                appViewModel = appViewModel,
                modifier = Modifier.animateItemPlacement(
                    tween(durationMillis = 300, delayMillis = index * 50)
                )
            )
        }
    }
}

@Composable
private fun Header(currentDate: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Good ${getGreeting()}!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = currentDate.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun NoClassesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Column {
                Text("No Classes Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Enjoy your free day!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ClassCard(
    scheduleWithSubject: ScheduleWithSubject,
    onMarkAttendance: (ScheduleWithSubject, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val schedule = scheduleWithSubject.schedule
    val subject = scheduleWithSubject.subject
    val cardColor = if (scheduleWithSubject.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(subject.color)))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val timeText = "${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}"
                Text(timeText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedContent(targetState = scheduleWithSubject.isCompleted, label = "attendance_buttons") { isCompleted ->
                if (isCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = "Attended", tint = SuccessGreen)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onMarkAttendance(scheduleWithSubject, true) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Check, "Present", tint = SuccessGreen)
                        }
                        IconButton(onClick = { onMarkAttendance(scheduleWithSubject, false) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Absent", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectOverviewCard(
    subject: Subject,
    onClick: () -> Unit,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var percentage by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(subject.id) { percentage = appViewModel.getAttendancePercentage(subject.id) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(subject.color)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color(android.graphics.Color.parseColor(subject.color)))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Target: ${subject.targetAttendance}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            percentage?.let {
                val color = if (it >= subject.targetAttendance) SuccessGreen else MaterialTheme.colorScheme.onSurface
                Text("${"%.1f".format(it)}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

private fun getGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Morning"
    in 12..16 -> "Afternoon"
    else -> "Evening"
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time).lowercase()
}
