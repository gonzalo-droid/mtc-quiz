package com.gondroid.mtcquiz.presentation.screens.evaluation.summary

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationDataState
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenAction
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenViewModel
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme


@Composable
fun SummaryScreenRoot(
    viewModel: EvaluationScreenViewModel,
) {
    val state = viewModel.state

    SummaryScreen(
        state = state,
        onAction = { action ->
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: EvaluationDataState,
    onAction: (EvaluationScreenAction) -> Unit
) {

    val angleRatio =
        remember {
            Animatable(0f)
        }

    val completedTasks = 30
    val totalTask = 40

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
                        text = "Resultados",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        modifier = Modifier,
                        text = "Marc 9, 2023",
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
                    text = "${(completedTasks / totalTask.toFloat()).times(100).toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = "Total correctas",
                count = state.correctAnswers.toString(),
                color = Color.Green
            )
            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = "Toral incorrectas",
                count = state.incorrectAnswers.toString(),
                color = Color.Red
            )
            ItemTotal(
                modifier = Modifier.fillMaxWidth(),
                label = "Total",
                count = state.questions.size.toString(),
                color = Color.Red
            )


            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(

            )
        }

    }
}

@Composable
fun ItemTotal(modifier: Modifier, color: Color, label: String, count: String) {
    Row(
        modifier = modifier
    ) {
        Text(
            color = color,
            modifier = Modifier,
            text = label,
            fontSize = 15.sp,

            )
        Spacer(Modifier.weight(1f))
        Text(
            color = color,
            modifier = Modifier,
            text = count,
            fontSize = 15.sp,

            )
    }
}

@Composable
fun ButtonsAction(
    onGoToEvaluation: () -> Unit = {},
    onGoToQuestions: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGoToEvaluation, modifier = Modifier
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Finalizar")
        }

        OutlinedButton(
            onClick = onGoToQuestions, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Estudiar")
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
            state = EvaluationDataState(),
            onAction = {}
        )
    }
}