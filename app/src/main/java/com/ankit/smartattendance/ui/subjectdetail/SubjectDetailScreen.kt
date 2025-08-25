package com.ankit.smartattendance.ui.subjectdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.RecordType
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.utils.HapticFeedbackManager
import com.ankit.smartattendance.viewmodel.AppViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(subjectId: Long, navController: NavController, appViewModel: AppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var subject by remember { mutableStateOf<Subject?>(null) }
    var stats by remember { mutableStateOf(Pair(0.0, 0)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val attendanceRecords by appViewModel.getAttendanceRecordsForSubject(subjectId)
        .collectAsState(initial = emptyList())
    val confettiTrigger by appViewModel.confettiTrigger.collectAsState()

    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(confettiTrigger) {
        if (confettiTrigger > 0) {
            showConfetti = true
        }
    }

    LaunchedEffect(subjectId, attendanceRecords) {
        coroutineScope.launch {
            subject = appViewModel.getSubjectById(subjectId)
            val total = appViewModel.getTotalClassesForSubject(subjectId)
            val present = appViewModel.getPresentClassesForSubject(subjectId)
            val percentage = if (total > 0) {
                (present.toDouble() / total) * 100
            } else {
                0.0
            }
            stats = Pair(percentage, total)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Details") },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                },
                actions = {
                    IconButton({ showManualAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            "Add Manual Attendance"
                        )
                    }
                    IconButton({ navController.navigate("edit_subject/$subjectId") }) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit"
                        )
                    }
                    IconButton({ showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }) { paddingValues ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    subject?.let {
                        AttendanceProgressCard(
                            it.name,
                            stats.first,
                            it.targetAttendance
                        )
                    }
                }
                item {
                    Text(
                        "Attendance History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AttendanceCalendar(
                        records = attendanceRecords,
                        onDayClick = { date ->
                            if (!date.isAfter(LocalDate.now())) {
                                HapticFeedbackManager.performHapticFeedback(context)
                                selectedDate = date
                            }
                        }
                    )
                }
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 30f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                        position = Position.Relative(0.5, 0.3)
                    )
                )
            )
            LaunchedEffect(Unit) {
                delay(3000)
                showConfetti = false
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

        if (showManualAddDialog) {
            ManualAddAttendanceDialog(
                onDismiss = { showManualAddDialog = false },
                onConfirm = { present, absent ->
                    appViewModel.addManualAttendance(subjectId, present, absent)
                    showManualAddDialog = false
                }
            )
        }

        selectedDate?.let { date ->
            MarkAttendanceDialog(
                date = date,
                onDismiss = { selectedDate = null },
                onConfirm = { isPresent ->
                    appViewModel.markAttendanceForDate(subjectId, date, isPresent)
                    selectedDate = null
                }
            )
        }
    }
}

@Composable
private fun MarkAttendanceDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Boolean?) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Attendance") },
        text = { Text("Set attendance status for ${date.format(formatter)}:") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onConfirm(null) }) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(true) }) { Text("Present") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Absent") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManualAddAttendanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (present: Int, absent: Int) -> Unit
) {
    var presentCount by remember { mutableStateOf("") }
    var absentCount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Past Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the number of classes held before you started using the app.")
                OutlinedTextField(
                    value = presentCount,
                    onValueChange = { presentCount = it.filter { c -> c.isDigit() } },
                    label = { Text("Classes Attended") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = absentCount,
                    onValueChange = { absentCount = it.filter { c -> c.isDigit() } },
                    label = { Text("Classes Missed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val present = presentCount.toIntOrNull() ?: 0
                val absent = absentCount.toIntOrNull() ?: 0
                if (present > 0 || absent > 0) {
                    onConfirm(present, absent)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AttendanceProgressCard(subjectName: String, percentage: Double, target: Int) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(durationMillis = 1000), label = ""
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text(
                subjectName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${"%.1f".format(animatedPercentage)}%",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target: $target%", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = animatedPercentage / 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun AttendanceCalendar(
    records: List<AttendanceRecord>,
    onDayClick: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    Column {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        MonthTitle(month = visibleMonth)

        val daysOfWeek = remember {
            val days = DayOfWeek.values()
            days.slice(days.indexOf(firstDayOfWeek)..days.lastIndex) + days.slice(
                0 until days.indexOf(
                    firstDayOfWeek
                )
            )
        }
        DaysOfWeekTitle(daysOfWeek = daysOfWeek)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalCalendar(state = state, dayContent = { day ->
            val recordForDay = remember(day, records) {
                records.find {
                    it.date != 0L && LocalDate.ofEpochDay(it.date) == day.date && it.type == RecordType.CLASS
                }
            }
            val dayBackgroundColor = when {
                recordForDay == null -> Color.Transparent
                recordForDay.isPresent -> SuccessGreen.copy(alpha = 0.4f)
                else -> ErrorRed.copy(alpha = 0.4f)
            }
            Box(
                Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color = dayBackgroundColor)
                    .clickable { onDayClick(day.date) },
                contentAlignment = Alignment.Center
            ) {
                val textColor = when {
                    day.date > LocalDate.now() -> MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.4f
                    )
                    else -> LocalContentColor.current
                }
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = textColor
                )
            }
        })
    }
}

@Composable
private fun MonthTitle(month: YearMonth) {
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val year = month.year
    Text(
        text = "$monthName $year",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
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
