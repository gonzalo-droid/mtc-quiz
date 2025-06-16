package com.gondroid.evaluation.presentation.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay


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
    onAction: (SummaryAction) -> Unit
) {

    val angleRatio =
        remember {
            Animatable(0f)
        }

    val completedTasks = state.evaluation.totalCorrect
    val totalTask = state.evaluation.totalQuestions

    var startPercentageAnimation by remember { mutableStateOf(false) }
    val percentage = (completedTasks / totalTask.toFloat()).times(100).toInt()
    val animatedPercentageValue by animateIntAsState(
        targetValue = if (startPercentageAnimation) percentage else 0,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "Counter Animation"
    )

    LaunchedEffect(completedTasks, totalTask) {
        if (totalTask == 0) {
            angleRatio.animateTo(
                targetValue = 0f,
            )
            return@LaunchedEffect
        }
        angleRatio.animateTo(
            targetValue = (completedTasks.toFloat() / totalTask.toFloat()),
            animationSpec =
                tween(
                    durationMillis = 300,
                ),
        )

        startPercentageAnimation = true
    }

    val pathJson =
        if (state.evaluation.state == EvaluationState.APPROVED) "approved_anim" else "reject_anim"
    val animationExam by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("anim/$pathJson.json")
    )
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000) // Espera 3 segundos
        isVisible = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.category.title,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleSmall.fontSize
                    )
                },
                navigationIcon = {

                },
                actions = {

                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.results),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        modifier = Modifier,
                        text = state.date.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )

                }
                Image(
                    painter = painterResource(id = R.drawable.card_background),
                    contentDescription = "card_background",
                    modifier = Modifier.weight(1f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            ProgressReport(
                modifier = Modifier
                    .width(200.dp)
                    .padding(16.dp)
                    .aspectRatio(1f)
                    .weight(1f)
                    .align(Alignment.CenterHorizontally),
                state = state,
                angleRatio = angleRatio.value,
                animatedPercentageValue = animatedPercentageValue
            )

            Spacer(modifier = Modifier.height(30.dp))

            TitleResult(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = state,
            )

            TotalSection(modifier = Modifier.fillMaxWidth(), state = state)

            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(
                onGoToDetail = { onAction(SummaryAction.FinishExam) },
            )
        }

        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = animationExam,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ProgressReport(
    modifier: Modifier,
    state: SummaryState,
    angleRatio: Float,
    animatedPercentageValue: Int
) {
    val completedTasks = state.evaluation.totalCorrect
    val totalTask = state.evaluation.totalQuestions

    val color =
        if (state.evaluation.state == EvaluationState.APPROVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        val colorBase = MaterialTheme.colorScheme.inversePrimary
        val progress = color
        val strokeWidth = 16.dp

        Canvas(
            modifier = Modifier.aspectRatio(1f),
        ) {
            drawArc(
                color = colorBase,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                size = size,
                style =
                    Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round,
                    ),
            )

            if (completedTasks <= totalTask) {
                drawArc(
                    color = progress,
                    startAngle = 90f,
                    sweepAngle = 360f * angleRatio,
                    useCenter = false,
                    size = size,
                    style =
                        Stroke(
                            width = strokeWidth.toPx(),
                            cap = StrokeCap.Round,
                        ),
                )
            }
        }

        Text(
            text = "${animatedPercentageValue}%",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun TitleResult(
    modifier: Modifier,
    state: SummaryState,
) {

    val isApproved = state.evaluation.state == EvaluationState.APPROVED

    val title =
        if (isApproved) stringResource(R.string.approved) else stringResource(R.string.rejected)
    val icon = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Close
    val color =
        if (isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .width(30.dp)
                .height(30.dp),
            imageVector = icon,
            contentDescription = "PlayCircle",
            tint = color
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            modifier = Modifier,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )

    }
}

@Composable
fun TotalSection(
    modifier: Modifier,
    state: SummaryState,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ItemTotal(
                modifier = modifier,
                label = stringResource(R.string.total_correct),
                count = state.evaluation.totalCorrect.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            ItemTotal(
                modifier = modifier,
                label = stringResource(R.string.total_incorrect),
                count = state.evaluation.totalIncorrect.toString(),
                color = MaterialTheme.colorScheme.error
            )
            ItemTotal(
                modifier = modifier,
                label = stringResource(R.string.total_question),
                count = state.evaluation.totalQuestions.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textSize = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun ItemTotal(
    modifier: Modifier,
    color: Color,
    label: String,
    count: String,
    textSize: TextStyle = MaterialTheme.typography.titleMedium
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedValue by animateIntAsState(
        targetValue = if (startAnimation) count.toInt() else 0,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing), // Duración 1s
        label = "Counter Animation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            style = textSize,
            color = color,
            modifier = Modifier,
            text = label,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            style = textSize,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier,
            text = animatedValue.toString(),
        )
    }
}


@Composable
fun ButtonsAction(
    onGoToDetail: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGoToDetail,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.finish_evaluation))
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewSummaryScreenRoot() {
    MTCQuizTheme {
        SummaryScreen(
            state = SummaryState(),
            onAction = {}
        )
    }
}