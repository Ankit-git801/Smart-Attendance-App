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
import com.ankit.smartattendance.ui.theme.ErrorRed
import com.ankit.smartattendance.ui.theme.SuccessGreen
import com.ankit.smartattendance.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(appViewModel: AppViewModel) {
    var stats by remember { mutableStateOf<AttendanceStatistics?>(null) }
    val subjects by appViewModel.allSubjects.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(subjects) {
        coroutineScope.launch {
            stats = appViewModel.getOverallStatistics()
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
                    stats?.let {
                        OverallPerformanceCard(it)
                    } ?: Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

                item {
                    Text(
                        "Subject Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(subjects) { subject ->
                    SubjectStatCard(subject, appViewModel)
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
        Icon(Icons.Default.BarChart, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(
            "No Statistics Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Add a subject and mark some attendance to see your stats here.",
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
                StatItem(Icons.Default.Functions, "Total", stats.totalClasses.toString())
                StatItem(Icons.Default.CheckCircle, "Present", stats.totalPresent.toString(), SuccessGreen)
                StatItem(Icons.Default.Cancel, "Absent", stats.totalAbsent.toString(), ErrorRed)
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
        animationSpec = tween(durationMillis = 1000)
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
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectStatCard(subject: Subject, appViewModel: AppViewModel) {
    var percentage by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(subject.id) { percentage = appViewModel.getAttendancePercentage(subject.id) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(android.graphics.Color.parseColor(subject.color)), CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.SemiBold)
                Text("Target: ${subject.targetAttendance}%", style = MaterialTheme.typography.bodySmall)
            }
            percentage?.let {
                val color = if (it >= subject.targetAttendance) SuccessGreen else ErrorRed
                Text(
                    "${"%.1f".format(it)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
