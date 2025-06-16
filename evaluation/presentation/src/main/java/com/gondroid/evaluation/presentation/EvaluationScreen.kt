package com.gondroid.evaluation.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Question
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import kotlinx.coroutines.delay


@Composable
fun EvaluationScreenRoot(
    viewModel: EvaluationScreenViewModel,
    navigateBack: () -> Boolean,
    navigateToSummary: (String, String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val event = viewModel.event
    var showCancelDialog by remember { mutableStateOf(false) }
    var blockExit by remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                is EvaluationEvent.EvaluationCreated -> {
                    navigateToSummary(state.category.id, event.evaluationId)
                }
            }
        }
    }

    BackHandler(enabled = blockExit) {
        showCancelDialog = true
    }

    EvaluationScreen(
        state = state, onAction = { action ->
            when (action) {
                EvaluationAction.Back -> {
                    showCancelDialog = true
                    blockExit = false
                }

                EvaluationAction.VerifyAnswer -> viewModel.verifyAnswer()
                EvaluationAction.NextQuestion -> viewModel.nextQuestion()
                is EvaluationAction.SaveAnswer -> viewModel.saveAnswer(
                    isCorrect = action.isCorrect, option = action.option
                )

                is EvaluationAction.SummaryExam -> {
                    viewModel.saveExam()
                    showCancelDialog = false
                    blockExit = false
                }

                EvaluationAction.ConfirmCancel -> {
                    showCancelDialog = false
                    blockExit = false
                    navigateBack()
                }

                EvaluationAction.DismissDialog -> {
                    showCancelDialog = false
                    blockExit = true
                }
            }
        }, showCancelDialog = showCancelDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    state: EvaluationState,
    onAction: (EvaluationAction) -> Unit,
    showCancelDialog: Boolean,
) {

    val progress by remember(state.indexQuestion) {
        derivedStateOf {
            if (state.questions.size > 1) {
                (state.indexQuestion.toFloat() / (state.questions.size - 1).coerceAtLeast(1)).coerceIn(
                    0f,
                    1f
                )
            } else {
                0f
            }
        }
    }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    val isCorrectAnswerSelected =
        selectedOption?.equals(state.question.getOption(state.question.answer)) == true

    var timeLeft by remember { mutableStateOf<Int?>(null) }
    var showFinishEvaluationDialog by remember { mutableStateOf(false) }


    LaunchedEffect(state.totalMinutes) {
        val totalMinutes = state.totalMinutes
        if (totalMinutes > 0) {
            val totalSeconds = totalMinutes * 60
            timeLeft = totalSeconds

            for (i in totalSeconds downTo 1) {
                delay(1000L)
                timeLeft = i - 1
            }

            showFinishEvaluationDialog = true
        }
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            onAction(
                                EvaluationAction.Back,
                            )
                        },
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {}
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            text = timeLeft?.toFormattedTime() ?: "00:00"
                        )

                    }
                },
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
        ) {

            LinearProgressComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                progress = progress,
                countProgress = "${state.indexQuestion + 1}/${state.questions.size}"
            )

            Spacer(modifier = Modifier.height(32.dp))

            CardQuestion(
                modifier = Modifier.fillMaxWidth(),
                question = state.question
            )


            LazyColumn(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom
            ) {
                itemsIndexed(state.question.options) { index, option ->
                    AnswerCard(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = option,
                        isCorrect = state.question.isCorrectAnswer(index),
                        isSelected = selectedOption == option,
                        isCorrectAnswerSelected = if (state.answerWasSelected) isCorrectAnswerSelected else null,
                        onClick = {
                            selectedOption = option
                        })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonsAction(
                selectedOption = selectedOption,
                state = state,
                modifier = Modifier.fillMaxWidth(),
                onClickNextQuestion = { type ->
                    when (type) {
                        VERIFY -> {
                            onAction(EvaluationAction.VerifyAnswer)
                        }

                        NEXT -> {
                            onAction(
                                EvaluationAction.SaveAnswer(
                                    option = selectedOption.toString(),
                                    isCorrect = isCorrectAnswerSelected == true
                                )
                            )
                            selectedOption = null
                            onAction(EvaluationAction.NextQuestion)
                        }

                        FINISH -> {
                            onAction(
                                EvaluationAction.SaveAnswer(
                                    option = selectedOption.toString(),
                                    isCorrect = isCorrectAnswerSelected == true
                                )
                            )
                            selectedOption = null
                            onAction(EvaluationAction.SummaryExam(state.category.id))
                        }
                    }
                })
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showFinishEvaluationDialog) {
            FinishedTimeDialog {
                showFinishEvaluationDialog = false
                onAction(EvaluationAction.SummaryExam(state.category.id))
            }
        }

        if (showCancelDialog) {
            CancelEvaluation(onConfirmCancel = {
                onAction(
                    EvaluationAction.ConfirmCancel
                )
            }, onDismiss = {
                onAction(
                    EvaluationAction.DismissDialog
                )
            })
        }

    }
}

@Composable
fun AnswerCard(
    state: EvaluationState,
    text: String,
    isCorrect: Boolean,
    isSelected: Boolean,
    isCorrectAnswerSelected: Boolean?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (backgroundColor, borderColor) = when {
        state.answerWasSelected -> when {
            isSelected && isCorrect -> Color(0xFFC8E6C9) to Color(0xFF388E3C)
            isSelected && !isCorrect -> Color(0xFFFFCDD2) to Color(0xFFD32F2F)
            isCorrectAnswerSelected == false && isCorrect -> Color(0xFFC8E6C9) to Color(0xFF388E3C)
            else -> Color.White to Color.Gray
        }

        isSelected -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiary
        else -> Color.White to Color.Gray
    }

    CardAnswer(
        modifier = modifier.clickable(enabled = isCorrectAnswerSelected == null) {
            onClick()
        },
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        text = text
    )
}


@Composable
fun ButtonsAction(
    state: EvaluationState,
    modifier: Modifier,
    onClickNextQuestion: (type: TypeActionQuestion) -> Unit = {},
    selectedOption: String?,
) {
    var typeQuestion = VERIFY

    val buttonText = when {
        state.isFinishExam -> {
            typeQuestion = FINISH
            stringResource(R.string.finish)
        }

        !state.answerWasVerified -> {
            typeQuestion = VERIFY
            stringResource(R.string.verify)
        }

        else -> {
            typeQuestion = NEXT
            stringResource(R.string.next)
        }
    }

    Column(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            enabled = !selectedOption.isNullOrEmpty(), onClick = {
                onClickNextQuestion(typeQuestion)
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = buttonText)
        }

    }
}


@Composable
fun FinishedTimeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.time_evaluation_to_finished)) },
        text = { Text(stringResource(R.string.subtitle_to_finished_evaluation)) },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.finish_evaluation))
            }
        },
        dismissButton = null
    )
}

@Composable
fun CancelEvaluation(
    onConfirmCancel: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                stringResource(R.string.cancel_evaluation),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { Text("Si cancelas ahora, se perderá todo el progreso de tu evaluación.") },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onConfirmCancel, modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.yes_cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        },
        dismissButton = null
    )
}


@Preview(
    showBackground = true,
)
@Composable
fun PreviewEvaluationScreenRoot() {

    val answers = listOf(
        "a) Recoger o dejar pasajeros o carga en cualquier lugar",
        "b) Recoger o dejar pasajeros o carga en cualquier lugar",
        "c) Recoger o dejar pasajeros o carga en cualquier lugar",
        "d) Recoger o dejar pasajeros o carga en cualquier lugar"
    )

    val question = Question(
        id = 1,
        title = "Respecto de los 100 de control o regulación del tránsito:",
        topic = "",
        section = "",
        options = answers,
        answer = "a",
    )

    MTCQuizTheme {
        EvaluationScreen(
            state = EvaluationState(
                questions = listOf(question), question = question, category = Category(
                    title = "CLASE A - CATEGORIA I"
                )
            ), onAction = {}, showCancelDialog = false
        )
    }
}