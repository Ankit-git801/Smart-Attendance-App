package com.ankit.smartattendance.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController, appViewModel: AppViewModel) {
    val subjectsWithAttendance by appViewModel.subjectsWithAttendance.collectAsState()
    var stats by remember { mutableStateOf<AttendanceStatistics?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(subjectsWithAttendance) {
        coroutineScope.launch {
            stats = appViewModel.getOverallStatistics()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Attendance Statistics") })
        }
    ) { paddingValues ->
        if (subjectsWithAttendance.isEmpty()) {
            EmptyState(Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    stats?.let {
                        OverallPerformanceCard(it)
                    } ?: Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                item {
                    Text(
                        "Subject Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(subjectsWithAttendance, key = { it.subject.id }) { subjectWithAttendance ->
                    SubjectStatCard(
                        navController = navController,
                        subjectWithAttendance = subjectWithAttendance
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.BarChart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "No Statistics Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add subjects and mark attendance to see your progress here. Your overall and subject-wise statistics will appear once you have some data.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OverallPerformanceCard(stats: AttendanceStatistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "Overall Performance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(percentage = stats.overallPercentage.toFloat())
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(
                    label = "Total",
                    value = stats.totalClasses.toString(),
                    icon = Icons.Default.Functions
                )
                StatItem(
                    label = "Present",
                    value = stats.totalPresent.toString(),
                    color = SuccessGreen,
                    icon = Icons.Default.CheckCircle
                )
                StatItem(
                    label = "Absent",
                    value = stats.totalAbsent.toString(),
                    color = ErrorRed,
                    icon = Icons.Default.Cancel
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    percentage: Float,
    radius: Dp = 80.dp,
    strokeWidth: Dp = 16.dp
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000),
        label = "donut_chart_progress"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.size(radius * 2)) {
            val sweepAngle = (animatedPercentage / 100) * 360f
            drawArc(
                color = SuccessGreen.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = SuccessGreen,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${"%.1f".format(animatedPercentage)}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text("Present", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) {
            Icon(icon, null, tint = color)
        }
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectStatCard(
    navController: NavController,
    subjectWithAttendance: SubjectWithAttendance
) {
    val subject = subjectWithAttendance.subject
    val percentage = subjectWithAttendance.percentage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("subject_detail/${subject.id}") }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(subject.color)),
                        CircleShape
                    )
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "Target: ${subject.targetAttendance}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "${subjectWithAttendance.presentClasses}/${subjectWithAttendance.totalClasses}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )

            val color = if (percentage >= subject.targetAttendance) SuccessGreen else ErrorRed
            Row(
                modifier = Modifier.width(65.dp), // Fixed width to ensure alignment
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom // Use Bottom alignment
            ) {
                Text(
                    text = "%.1f".format(percentage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.alignByBaseline() // Aligns text by its baseline
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .alignByBaseline() // Aligns this text by its baseline
                )
            }
        }
    }
}
