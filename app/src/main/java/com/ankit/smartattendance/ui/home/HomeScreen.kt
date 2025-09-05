package com.ankit.smartattendance.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ankit.smartattendance.data.RecordType
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.PoppinsFamily
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

private data class GreetingInfo(
    val greetingText: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

private fun getGreetingInfo(): GreetingInfo {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> GreetingInfo(
            "Good Morning",
            Icons.Outlined.WbSunny,
            listOf(Color(0xFFFBE9A7), Color(0xFFFAD9A3))
        )
        in 12..16 -> GreetingInfo(
            "Good Afternoon",
            Icons.Outlined.Brightness5,
            listOf(Color(0xFF81D4FA), Color(0xFF89CFF0))
        )
        in 17..20 -> GreetingInfo(
            "Good Evening",
            Icons.Outlined.NightsStay,
            listOf(Color(0xFF7D7AFF), Color(0xFF5356FF))
        )
        else -> GreetingInfo(
            "Good Night",
            Icons.Outlined.NightsStay,
            listOf(Color(0xFF303158), Color(0xFF232347))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjectsWithAttendance by appViewModel.subjectsWithAttendance.collectAsState()
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsState()
    val userName by appViewModel.userName.collectAsState()

    var showExtraClassConfirmationDialog by remember { mutableStateOf(false) }
    var showExtraClassSubjectDialog by remember { mutableStateOf(false) }

    if (showExtraClassConfirmationDialog) {
        ExtraClassConfirmationDialog(
            onDismiss = { showExtraClassConfirmationDialog = false },
            onConfirm = {
                showExtraClassConfirmationDialog = false
                showExtraClassSubjectDialog = true
            }
        )
    }

    if (showExtraClassSubjectDialog) {
        val allSubjects = subjectsWithAttendance.map { it.subject }
        ExtraClassSubjectDialog(
            subjects = allSubjects,
            onDismiss = { showExtraClassSubjectDialog = false },
            onConfirm = { subjectId, isPresent ->
                appViewModel.markExtraClassAttendance(subjectId, isPresent, "Extra Class")
                showExtraClassSubjectDialog = false
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GreetingCard(userName = userName)
                Spacer(modifier = Modifier.height(24.dp))
                QuickActions(
                    onExtraClassClick = { showExtraClassConfirmationDialog = true },
                    onNewSubjectClick = { navController.navigate("add_subject") }
                )
            }

            item {
                Text(
                    text = "TODAY'S CLASSES",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (todaysSchedule.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.WbSunny,
                        title = "No Classes Today",
                        subtitle = "You have no classes scheduled for today. Enjoy your day off!"
                    )
                }
            } else {
                items(todaysSchedule, key = { "schedule_${it.schedule.id}" }) { scheduleWithSubject ->
                    TodayScheduleCard(
                        scheduleWithSubject = scheduleWithSubject,
                        appViewModel = appViewModel,
                        onMark = { isPresent ->
                            if (isPresent) {
                                appViewModel.markTodayAsPresent(scheduleWithSubject.subject.id, scheduleWithSubject.schedule.id)
                            } else {
                                appViewModel.markTodayAsAbsent(scheduleWithSubject.subject.id, scheduleWithSubject.schedule.id)
                            }
                        },
                        onCancel = {
                            appViewModel.markTodayAsCancelled(scheduleWithSubject.subject.id, scheduleWithSubject.schedule.id)
                        },
                        onClick = {
                            navController.navigate("subject_detail/${scheduleWithSubject.subject.id}")
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ALL SUBJECTS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (subjectsWithAttendance.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.School,
                        title = "No Subjects Added",
                        subtitle = "Get started by adding your first subject. You can set a custom name, color, and attendance target for it.",
                        buttonText = "Add New Subject",
                        onButtonClick = { navController.navigate("add_subject") }
                    )
                }
            } else {
                items(subjectsWithAttendance, key = { "subject_${it.subject.id}" }) { subjectWithAttendance ->
                    SubjectCard(subjectWithAttendance = subjectWithAttendance) {
                        navController.navigate("subject_detail/${subjectWithAttendance.subject.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraClassConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Extra Class", fontFamily = PoppinsFamily) },
        text = { Text("Do you want to mark an extra class for today, $formattedDate?", fontFamily = PoppinsFamily) },
        confirmButton = {
            Button(onClick = onConfirm) {
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

@Composable
fun GreetingCard(userName: String) {
    val greetingInfo = getGreetingInfo()
    val textColor = if (greetingInfo.greetingText == "Good Night") Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(greetingInfo.gradientColors))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = greetingInfo.icon,
                contentDescription = "Greeting Icon",
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${greetingInfo.greetingText}, $userName",
                    style = MaterialTheme.typography.headlineSmall.copy(color = textColor)
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f))
                )
            }
        }
    }
}


@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(onClick = onButtonClick) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onExtraClassClick: () -> Unit, onNewSubjectClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlaylistAdd,
            text = "Extra Class",
            onClick = onExtraClassClick
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            text = "New Subject",
            onClick = onNewSubjectClick
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraClassSubjectDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, isPresent: Boolean) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Subject", fontFamily = PoppinsFamily) },
        text = {
            Column {
                if (subjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = subjects.find { it.id == selectedSubjectId }?.name
                                ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        selectedSubjectId = subject.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("Please add a subject first.")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, true) }
                    },
                    enabled = selectedSubjectId != null
                ) {
                    Text("Present")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        selectedSubjectId?.let { onConfirm(it, false) }
                    },
                    enabled = selectedSubjectId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Absent")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScheduleCard(
    scheduleWithSubject: ScheduleWithSubject,
    appViewModel: AppViewModel = viewModel(),
    onMark: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit
) {
    val records by appViewModel.allAttendanceRecords.collectAsState(initial = emptyList())
    var isAlreadyMarked by remember { mutableStateOf(false) }
    var recordType by remember { mutableStateOf<RecordType?>(null) }
    var wasPresent by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(records, scheduleWithSubject.subject.id) {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val recordForToday = records.find { it.subjectId == scheduleWithSubject.subject.id && it.date == todayEpochDay && it.scheduleId == scheduleWithSubject.schedule.id }
        isAlreadyMarked = recordForToday != null
        wasPresent = recordForToday?.isPresent
        recordType = recordForToday?.type
    }

    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = formatTime(schedule.startHour, schedule.startMinute)
    val endTime = formatTime(schedule.endHour, schedule.endMinute)
    val subjectColor = Color(android.graphics.Color.parseColor(subject.color))
    val isLive = scheduleWithSubject.isCurrentClass

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "$startTime - $endTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isLive && !isAlreadyMarked) {
                        LiveBadge()
                    }
                }

                AnimatedContent(
                    targetState = isAlreadyMarked,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300)) using
                                SizeTransform(clip = false)
                    },
                    modifier = Modifier.align(Alignment.End), label = "attendance_buttons"
                ) { marked ->
                    if (marked) {
                        val (icon, text, color) = when (recordType) {
                            RecordType.CANCELLED -> Triple(Icons.Filled.EventBusy, "Class Cancelled", MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> if (wasPresent == true) Triple(Icons.Filled.CheckCircle, "Marked as Present", SuccessGreen) else Triple(Icons.Filled.Cancel, "Marked as Absent", ErrorRed)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = color)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(onClick = { onMark(true) }) {
                                Text("Present")
                            }
                            OutlinedButton(onClick = { onMark(false) }) {
                                Text("Absent")
                            }
                            IconButton(
                                onClick = { onCancel() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Mark as Cancelled"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_badge_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "live_badge_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(ErrorRed.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(ErrorRed)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "LIVE",
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = PoppinsFamily
        )
    }
}
private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}

@Composable
fun SubjectCard(subjectWithAttendance: SubjectWithAttendance, onClick: () -> Unit) {
    val subject = subjectWithAttendance.subject
    val percentage = subjectWithAttendance.percentage
    val subjectColor = Color(android.graphics.Color.parseColor(subject.color))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Target: ${subject.targetAttendance}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${subjectWithAttendance.presentClasses} / ${subjectWithAttendance.totalClasses} classes attended",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            val animatedPercentage by animateFloatAsState(
                targetValue = percentage.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "subject_card_progress"
            )
            AnimatedCircularProgress(
                percentage = animatedPercentage,
                color = subjectColor
            )
        }
    }
}

@Composable
fun AnimatedCircularProgress(
    percentage: Float,
    color: Color,
    radius: Dp = 32.dp,
    strokeWidth: Dp = 4.dp
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (percentage / 100) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
