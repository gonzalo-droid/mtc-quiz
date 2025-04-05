package com.gondroid.mtcquiz.presentation.screens.evaluation

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.core.toFormattedTime
import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Question
import com.gondroid.mtcquiz.domain.models.TypeActionQuestion
import com.gondroid.mtcquiz.domain.models.TypeActionQuestion.FINISH
import com.gondroid.mtcquiz.domain.models.TypeActionQuestion.NEXT
import com.gondroid.mtcquiz.domain.models.TypeActionQuestion.VERIFY
import com.gondroid.mtcquiz.presentation.component.LinearProgressComponent
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import kotlinx.coroutines.delay


@Composable
fun EvaluationScreenRoot(
    viewModel: EvaluationScreenViewModel,
    navigateBack: () -> Boolean,
    navigateToSummary: (String, String) -> Unit,
) {

    val state = viewModel.state
    val event = viewModel.event

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                is EvaluationEvent.EvaluationCreated -> {
                    navigateToSummary(state.category.id, event.evaluationId)
                }
            }
        }
    }

    EvaluationScreen(
        state = state, onAction = { action ->
            when (action) {
                EvaluationScreenAction.Back -> navigateBack()
                EvaluationScreenAction.VerifyAnswer -> viewModel.verifyAnswer()
                EvaluationScreenAction.NextQuestion -> viewModel.nextQuestion()
                is EvaluationScreenAction.SaveAnswer -> viewModel.saveAnswer(
                    isCorrect = action.isCorrect,
                    option = action.option
                )

                is EvaluationScreenAction.SummaryExam -> viewModel.saveExam()
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    state: EvaluationDataState,
    onAction: (EvaluationScreenAction) -> Unit,
) {

    val progress by remember(state.indexQuestion) {
        derivedStateOf {
            if (state.questions.size > 1) {
                (state.indexQuestion.toFloat() / (state.questions.size - 1).coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    val isCorrectAnswerSelected =
        selectedOption?.equals(state.question.getOption(state.question.answer)) == true

    var timeLeft by remember { mutableIntStateOf(state.totalMinutes * 60) }
    var showFinishEvaluationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }

        if (timeLeft == 0) {
            showFinishEvaluationDialog = true
        }
        // onExamEnd()
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    var showCancelDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.d("Compose", "ON_START")
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.d("Compose", "ON_RESUME")
                }

                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("Compose", "ON_PAUSE")
                    showCancelDialog = true
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                                EvaluationScreenAction.Back,
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
                            text = timeLeft.toFormattedTime()
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

            QuestionCard(question = state.question, modifier = Modifier.fillMaxWidth())


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
                        isCorrect = state.question.validationAnswer(index),
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
                state = state, modifier = Modifier.fillMaxWidth(), onClickNextQuestion = { type ->
                    when (type) {
                        VERIFY -> {
                            onAction(EvaluationScreenAction.VerifyAnswer)
                        }

                        NEXT -> {
                            onAction(
                                EvaluationScreenAction.SaveAnswer(
                                    option = selectedOption.toString(),
                                    isCorrect = isCorrectAnswerSelected == true
                                )
                            )
                            selectedOption = null
                            onAction(EvaluationScreenAction.NextQuestion)
                        }

                        FINISH -> {
                            onAction(
                                EvaluationScreenAction.SaveAnswer(
                                    option = selectedOption.toString(),
                                    isCorrect = isCorrectAnswerSelected == true
                                )
                            )
                            selectedOption = null
                            onAction(EvaluationScreenAction.SummaryExam(state.category.id))
                        }
                    }
                })
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showFinishEvaluationDialog) {
            ExamEndDialog { showFinishEvaluationDialog = false }
        }

        if (showCancelDialog) {
            CancelEvaluation(
                onConfirmCancel = {
                    showCancelDialog = false
                    onAction(
                        EvaluationScreenAction.Back,
                    )
                },
                onDismiss = {
                    showCancelDialog = false
                }
            )
        }

    }
}

@Composable
fun QuestionCard(question: Question, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier,
                text = "${question.id}.- ${question.title}",
                style = MaterialTheme.typography.titleSmall,

                )
            Image(
                painter = painterResource(id = R.drawable.card_background),
                contentDescription = "card_background",
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        }
    }
}


@Composable
fun AnswerCard(
    state: EvaluationDataState,
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

    Card(
        modifier = modifier.clickable(enabled = isCorrectAnswerSelected == null) {
            onClick()
        },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = text, style = MaterialTheme.typography.titleSmall)
        }
    }
}


@Composable
fun ButtonsAction(
    state: EvaluationDataState,
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
            enabled = !selectedOption.isNullOrEmpty(),
            onClick = {
                onClickNextQuestion(typeQuestion)
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = buttonText)
        }

    }
}


@Composable
fun ExamEndDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Tiempo Finalizado") },
        text = { Text("Tu tiempo ha terminado. El examen se ha finalizado.") },
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
    onConfirmCancel: () -> Unit,
    onDismiss: () -> Unit
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
                    onClick = onConfirmCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.yes_cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
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
            state = EvaluationDataState(
                questions = listOf(question),
                question = question,
                category = Category(
                    title = "CLASE A - CATEGORIA I"
                )
            ), onAction = {})
    }
}