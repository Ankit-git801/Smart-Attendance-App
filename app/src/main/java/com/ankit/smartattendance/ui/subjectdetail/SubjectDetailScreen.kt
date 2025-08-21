package com.ankit.smartattendance.ui.subjectdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: Long,
    navController: NavController,
    appViewModel: AppViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var subject by remember { mutableStateOf<Subject?>(null) }
    var stats by remember { mutableStateOf<Pair<Double, Int>>(Pair(0.0, 0)) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val attendanceRecords by appViewModel.getAttendanceRecordsForSubject(subjectId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(subjectId, attendanceRecords) {
        coroutineScope.launch {
            subject = appViewModel.getSubjectById(subjectId)
            val total = appViewModel.getTotalClassesForSubject(subjectId)
            val present = appViewModel.getPresentClassesForSubject(subjectId)
            stats = Pair(if (total > 0) (present.toDouble() / total) * 100 else 100.0, total)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Subject") },
            text = { Text("Are you sure you want to delete '${subject?.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        subject?.let { appViewModel.deleteSubject(it) }
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton({ showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Details") },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton({ navController.navigate("edit_subject/$subjectId") }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton({ showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                subject?.let {
                    AttendanceProgressCard(it.name, stats.first, it.targetAttendance)
                }
            }
            item {
                Text("Attendance Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                AttendanceCalendar(records = attendanceRecords)
            }
        }
    }
}

@Composable
private fun AttendanceProgressCard(subjectName: String, percentage: Double, target: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text(subjectName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${"%.1f".format(percentage)}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target: $target%", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun AttendanceCalendar(records: List<AttendanceRecord>) {
    // CORRECTED: Use rememberCalendarState for a simple, non-selectable calendar.
    val calendarState = rememberCalendarState(
        initialMonth = YearMonth.now()
    )

    StaticCalendar(
        calendarState = calendarState,
        dayContent = { dayState ->
            val record = records.find { it.date == dayState.date.toEpochDay() }
            val dayBackgroundColor = when {
                record == null -> Color.Transparent
                record.isPresent -> SuccessGreen.copy(alpha = 0.3f)
                else -> ErrorRed.copy(alpha = 0.3f)
            }
            Box(
                Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(dayBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(dayState.date.dayOfMonth.toString())
            }
        }
    )
}
