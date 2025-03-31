package com.gondroid.mtcquiz.presentation.screens.evaluation.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme


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
                SummaryScreenAction.FinishExam -> navigateToDetail(state.category.id)
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryDataState,
    onAction: (SummaryScreenAction) -> Unit
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
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing), // Duración 1s
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

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .width(200.dp)
                        .padding(16.dp)
                        .aspectRatio(1f)
                        .weight(1f)
                        .align(Alignment.CenterHorizontally),
            ) {
                val colorBase = MaterialTheme.colorScheme.inversePrimary
                val progress = MaterialTheme.colorScheme.primary
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
                            sweepAngle = 360f * angleRatio.value,
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

            Spacer(modifier = Modifier.height(30.dp))

            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.total_correct),
                count = state.evaluation.totalCorrect.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.total_incorrect),
                count = state.evaluation.totalIncorrect.toString(),
                color = MaterialTheme.colorScheme.error
            )
            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.total_question),
                count = state.evaluation.totalQuestions.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(
                onGoToDetail = { onAction(SummaryScreenAction.FinishExam) },
            )
        }

    }
}

@Composable
fun ItemTotal(modifier: Modifier, color: Color, label: String, count: String) {
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
    ) {
        Text(
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier,
            text = label,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            style = MaterialTheme.typography.titleMedium,
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
            onClick = onGoToDetail, modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Finalizar evaluación")
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
            state = SummaryDataState(),
            onAction = {}
        )
    }
}