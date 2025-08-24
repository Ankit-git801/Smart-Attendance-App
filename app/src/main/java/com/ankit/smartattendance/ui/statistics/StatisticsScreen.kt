package com.ankit.smartattendance.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.viewmodel.AppViewModel
import kotlinx.coroutines.launch

// Data class to hold calculated stats for each subject for easier state management
private data class SubjectStats(
    val subject: Subject,
    val percentage: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(appViewModel: AppViewModel) {
    var overallStats by remember { mutableStateOf<AttendanceStatistics?>(null) }
    val subjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    var subjectStats by remember { mutableStateOf<List<SubjectStats>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Re-calculates stats when the list of subjects changes
    LaunchedEffect(subjects) {
        coroutineScope.launch {
            overallStats = appViewModel.getOverallStatistics()
            subjectStats = subjects.map { subject ->
                SubjectStats(
                    subject = subject,
                    percentage = appViewModel.getAttendancePercentage(subject.id)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Attendance Statistics") })
        }
    ) { paddingValues ->
        if (subjects.isEmpty()) {
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
                    overallStats?.let {
                        OverallPerformanceCard(it)
                    } ?: Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                item {
                    Text(
                        "Subject Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(subjectStats, key = { it.subject.id }) { stats ->
                    SubjectStatCard(stats = stats)
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
            Icons.Filled.QueryStats,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "No Statistics Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Add a subject and mark some attendance to see your progress here.",
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
            Text("Overall Performance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(percentage = stats.overallPercentage.toFloat())
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(Icons.Default.Functions, "Total Classes", stats.totalClasses.toString(), MaterialTheme.colorScheme.onSurface)
                StatItem(Icons.Default.CheckCircle, "Present", stats.totalPresent.toString(), Color(0xFF388E3C))
                StatItem(Icons.Default.Cancel, "Absent", stats.totalAbsent.toString(), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DonutChart(
    percentage: Float,
    radius: Dp = 90.dp,
    strokeWidth: Dp = 20.dp
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1200),
        label = "donutChartAnimation"
    )
    val color = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.size(radius * 2)) {
            val sweepAngle = (animatedPercentage / 100) * 360f
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${"%.1f".format(animatedPercentage)}%",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text("Attendance", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectStatCard(stats: SubjectStats) {
    val subjectColor = Color(android.graphics.Color.parseColor(stats.subject.color))
    val percentage = stats.percentage
    val target = stats.subject.targetAttendance

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(subjectColor, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(stats.subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = subjectColor
                )
                Spacer(Modifier.width(16.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = (percentage / 100).toFloat(),
                    animationSpec = tween(1000),
                    label = "subjectProgressBarAnimation"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = subjectColor,
                    trackColor = subjectColor.copy(alpha = 0.2f)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Target: $target%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
