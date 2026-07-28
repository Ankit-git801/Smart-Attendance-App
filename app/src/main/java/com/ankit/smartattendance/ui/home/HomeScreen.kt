package com.ankit.smartattendance.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ankit.smartattendance.data.BunkAnalysis
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
import java.time.format.DateTimeFormatter
import java.util.*

data class GreetingInfo(
    val greetingText: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

fun getGreetingInfo(): GreetingInfo {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        // Morning: 4:00 AM - 12:00 PM (Sunrise shade)
        in 4..11 -> GreetingInfo(
            "Good Morning",
            Icons.Default.WbSunny,
            listOf(Color(0xFFFF9800), Color(0xFFFFE082))
        )
        // Afternoon: 12:00 PM - 4:00 PM (Noon shade)
        in 12..15 -> GreetingInfo(
            "Good Afternoon",
            Icons.Default.WbCloudy,
            listOf(Color(0xFF00B0FF), Color(0xFF80D8FF))
        )
        // Early Evening: 4:00 PM - 8:00 PM (Current Blue-Orange shade)
        in 16..19 -> GreetingInfo(
            "Good Evening",
            Icons.Default.WbTwilight,
            listOf(Color(0xFCFF4E4E), Color(0xFFFFEB3B))
        )
        // Late Evening/Night: 8:00 PM - 4:00 AM (Good Evening with Night shade)
        else -> GreetingInfo(
            "Good Evening",
            Icons.Default.NightsStay,
            listOf(Color(0xFF311B92), Color(0xFF1A237E))
        )
    }
}

@Composable
fun HomeScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjectsWithAttendance by appViewModel.subjectsWithAttendance.collectAsStateWithLifecycle()
    val todaysSchedule by appViewModel.todaysScheduleWithSubjects.collectAsStateWithLifecycle()
    val userName by appViewModel.userName.collectAsStateWithLifecycle()
    val bunkAnalysisMap by appViewModel.bunkAnalysisMap.collectAsStateWithLifecycle()
    var showExtraClassDialog by remember { mutableStateOf(false) }

    val allSubjects by appViewModel.allSubjects.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GreetingCard(userName)
        }

        item {
            QuickActions(
                onNewSubjectClick = { navController.navigate("add_subject") },
                onExtraClassClick = { showExtraClassDialog = true }
            )
        }

        item {
            Text(
                "TODAY'S CLASSES",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (todaysSchedule.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.WbSunny,
                    title = "No Classes Today",
                    subtitle = "You have no classes scheduled for today. Enjoy your day off!"
                )
            }
        } else {
            items(todaysSchedule) { schedule ->
                TodayScheduleCard(
                    scheduleWithSubject = schedule,
                    appViewModel = appViewModel,
                    onClick = { navController.navigate("subject_detail/${schedule.subject.id}") }
                )
            }
        }

        item {
            Text(
                "ALL SUBJECTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (subjectsWithAttendance.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.MenuBook,
                    title = "No Subjects Yet",
                    subtitle = "Add your subjects to start tracking your attendance.",
                    actionLabel = "Add Subject",
                    onActionClick = { navController.navigate("add_subject") }
                )
            }
        } else {
            items(subjectsWithAttendance) { subjectWithAttendance ->
                SubjectCard(
                    subjectWithAttendance = subjectWithAttendance,
                    bunkAnalysis = bunkAnalysisMap[subjectWithAttendance.subject.id],
                    onClick = { navController.navigate("subject_detail/${subjectWithAttendance.subject.id}") }
                )
            }
        }
    }

    if (showExtraClassDialog) {
        ExtraClassDialog(
            subjects = allSubjects,
            onDismiss = { showExtraClassDialog = false },
            onConfirm = { subjectId, isPresent, count ->
                appViewModel.addExtraClasses(subjectId, LocalDate.now(), isPresent, count)
                showExtraClassDialog = false
            }
        )
    }
}

@Composable
fun ExtraClassDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: String, isPresent: Boolean, count: Int) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
    var isPresent by remember { mutableStateOf(true) }
    var count by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Extra Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (subjects.isEmpty()) {
                    Text("No subjects available. Add a subject first.")
                } else {
                    Text("Subject")
                    ScrollableTabRow(
                        selectedTabIndex = subjects.indexOfFirst { it.id == selectedSubjectId }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        subjects.forEach { subject ->
                            Tab(
                                selected = selectedSubjectId == subject.id,
                                onClick = { selectedSubjectId = subject.id },
                                text = { Text(subject.name) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status", modifier = Modifier.weight(1f))
                        FilterChip(
                            selected = isPresent,
                            onClick = { isPresent = true },
                            label = { Text("Present") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = !isPresent,
                            onClick = { isPresent = false },
                            label = { Text("Absent") }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Count", modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (count > 1) count-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { count++ }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSubjectId, isPresent, count) },
                enabled = subjects.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Add") }
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") } 
        }
    )
}

@Composable
fun GreetingCard(userName: String) {
    val greetingInfo = getGreetingInfo()
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = greetingInfo.gradientColors))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = greetingInfo.icon,
                    contentDescription = "Greeting Icon",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = "${greetingInfo.greetingText}, ${userName.ifEmpty { "Ankit" }} !",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = PoppinsFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun QuickActions(onNewSubjectClick: () -> Unit, onExtraClassClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlaylistAdd,
            title = "Extra Class",
            onClick = onExtraClassClick
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            title = "New Subject",
            onClick = onNewSubjectClick
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TodayScheduleCard(
    scheduleWithSubject: ScheduleWithSubject,
    appViewModel: AppViewModel,
    onClick: () -> Unit
) {
    val processingIds by appViewModel.processingScheduleIds.collectAsStateWithLifecycle()
    val isProcessing = processingIds.contains(scheduleWithSubject.schedule.id)
    
    val record = scheduleWithSubject.attendanceRecord
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "card_scale")

    // Use derivedStateOf to avoid UI flickering during recomposition
    val attendanceState by remember(record) {
        derivedStateOf {
            Triple(record != null, record?.type, record?.isPresent)
        }
    }
    val (isAlreadyMarked, recordType, wasPresent) = attendanceState

    val subject = scheduleWithSubject.subject
    val schedule = scheduleWithSubject.schedule
    val startTime = remember(schedule.startHour, schedule.startMinute) { formatTime(schedule.startHour, schedule.startMinute) }
    val endTime = remember(schedule.endHour, schedule.endMinute) { formatTime(schedule.endHour, schedule.endMinute) }
    val subjectColor = remember(subject.color) { Color(android.graphics.Color.parseColor(subject.color)) }
    val isLive = scheduleWithSubject.isCurrentClass

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = !isProcessing
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(subjectColor)
                )
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isAlreadyMarked,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300)) using
                                    SizeTransform(clip = false)
                        },
                        modifier = Modifier.fillMaxWidth(), label = "attendance_buttons"
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
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    FilledTonalButton(
                                        onClick = { appViewModel.markTodayAsPresent(subject.id, schedule.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isProcessing
                                    ) {
                                        Text("Present")
                                    }
                                    OutlinedButton(
                                        onClick = { appViewModel.markTodayAsAbsent(subject.id, schedule.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isProcessing
                                    ) {
                                        Text("Absent")
                                    }
                                    IconButton(
                                        onClick = { appViewModel.markTodayAsCancelled(subject.id, schedule.id) },
                                        enabled = !isProcessing,
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
    }
}

@Composable
fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.ExtraBold
            )
        }
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
fun SubjectCard(subjectWithAttendance: SubjectWithAttendance, bunkAnalysis: BunkAnalysis?, onClick: () -> Unit) {
    val subject = subjectWithAttendance.subject
    val percentage = subjectWithAttendance.percentage
    val subjectColor = remember(subject.color) { Color(android.graphics.Color.parseColor(subject.color)) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "card_scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(subjectColor, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFamily
                    )
                }
                
                CircularProgressIndicator(
                    progress = { (percentage / 100f).toFloat() },
                    modifier = Modifier.size(48.dp),
                    color = subjectColor,
                    strokeWidth = 6.dp,
                    trackColor = subjectColor.copy(alpha = 0.2f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${percentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentage < subject.targetAttendance) ErrorRed else SuccessGreen
                    )
                    Text(
                        text = "Attendance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (bunkAnalysis != null) {
                    BunkAnalysisText(bunkAnalysis)
                }
            }
        }
    }
}

@Composable
fun BunkAnalysisText(analysis: BunkAnalysis) {
    val text = when {
        analysis.classesToBunk > 0 -> "You can bunk ${analysis.classesToBunk} classes."
        analysis.classesToAttend > 0 -> "Attend next ${analysis.classesToAttend} classes."
        else -> "On track!"
    }
    val color = if (analysis.classesToBunk > 0) SuccessGreen else if (analysis.classesToAttend > 0) ErrorRed else MaterialTheme.colorScheme.primary

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
