package com.gondroid.evaluation.presentation.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.core.domain.model.EvaluationState
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.evaluation.presentation.R
import kotlinx.coroutines.delay

private val approvedColor = Color(0xFF4CAF50)
private val rejectedColor = Color(0xFFE53935)

@Composable
fun SummaryScreenRoot(
    viewModel: SummaryScreenViewModel,
    navigateToDetail: (String) -> Boolean,
) {
    val state = viewModel.state
    SummaryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SummaryAction.FinishExam -> navigateToDetail(state.category.id)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryState,
    onAction: (SummaryAction) -> Unit,
) {
    val isApproved = state.evaluation.state == EvaluationState.APPROVED
    val accentColor = if (isApproved) approvedColor else rejectedColor

    val completedTasks = state.evaluation.totalCorrect
    val totalTask = state.evaluation.totalQuestions
    val percentage = if (totalTask > 0) (completedTasks / totalTask.toFloat()).times(100).toInt() else 0

    val angleRatio = remember { Animatable(0f) }
    var startCounterAnim by remember { mutableStateOf(false) }
    val animatedPercentage by animateIntAsState(
        targetValue = if (startCounterAnim) percentage else 0,
        animationSpec = tween(durationMillis = 1200, easing = LinearEasing),
        label = "percentage",
    )
    val animatedCorrect by animateIntAsState(
        targetValue = if (startCounterAnim) completedTasks else 0,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "correct",
    )
    val animatedIncorrect by animateIntAsState(
        targetValue = if (startCounterAnim) state.evaluation.totalIncorrect else 0,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "incorrect",
    )

    var showContent by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(completedTasks, totalTask) {
        if (totalTask > 0) {
            angleRatio.animateTo(
                targetValue = completedTasks.toFloat() / totalTask.toFloat(),
                animationSpec = tween(durationMillis = 1200),
            )
        }
        startCounterAnim = true
        delay(200)
        showContent = true
        delay(400)
        showStats = true
        delay(300)
        showButton = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.category.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .semantics { contentDescription = "$percentage por ciento de respuestas correctas" },
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * angleRatio.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedPercentage}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Status badge
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it / 2 },
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isApproved) stringResource(R.string.approved) else stringResource(R.string.rejected),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(500)),
            ) {
                Text(
                    text = state.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(28.dp))

            // Stats cards
            AnimatedVisibility(
                visible = showStats,
                enter = fadeIn() + slideInVertically { it / 3 },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatResultCard(
                        label = stringResource(R.string.total_correct),
                        value = animatedCorrect.toString(),
                        color = approvedColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatResultCard(
                        label = stringResource(R.string.total_incorrect),
                        value = animatedIncorrect.toString(),
                        color = rejectedColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatResultCard(
                        label = stringResource(R.string.total_question),
                        value = totalTask.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Message
            AnimatedVisibility(
                visible = showStats,
                enter = fadeIn(animationSpec = tween(600)),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = if (isApproved)
                            "¡Felicidades! Estás listo para rendir el examen del MTC."
                        else
                            "Sigue practicando. Repasa tus errores frecuentes para mejorar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 22.sp,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn() + slideInVertically { it },
            ) {
                Button(
                    onClick = { onAction(SummaryAction.FinishExam) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Finalizar evaluación" },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.finish_evaluation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatResultCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSummaryScreenRoot() {
    MTCQuizTheme {
        SummaryScreen(
            state = SummaryState(),
            onAction = {},
        )
    }
}
