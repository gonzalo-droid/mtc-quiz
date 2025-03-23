package com.gondroid.mtcquiz.presentation.screens.evaluation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.domain.models.Question
import com.gondroid.mtcquiz.presentation.component.LinearProgressComponent
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme


@Composable
fun EvaluationScreenRoot(
    viewModel: EvaluationScreenViewModel,
    navigateBack: () -> Boolean,
) {

    val state = viewModel.state


    EvaluationScreen(
        state = state,
        onAction = { action ->
            when (action) {
                EvaluationScreenAction.Back -> navigateBack()
                EvaluationScreenAction.StartEvaluation -> TODO()
                EvaluationScreenAction.NextQuestion -> {
                    viewModel.nextQuestion()
                }
            }

        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    state: EvaluationDataState,
    onAction: (EvaluationScreenAction) -> Unit,
) {
    val totalQuestions = state.questions.size

    val progress = remember {
        derivedStateOf {
            if (totalQuestions > 0) {
                state.indexQuestion.toFloat() / (totalQuestions - 1).coerceAtLeast(1)
            } else {
                0f
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CLASE A - CATEGORÍA I",
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
                        modifier =
                            Modifier.clickable {
                                onAction(
                                    EvaluationScreenAction.Back,
                                )
                            },
                    )
                },
                actions = {
                    Box(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            text = "29:30"
                        )

                    }
                },
            )
        },
    ) { paddingValues ->

        var selectedOption by remember { mutableStateOf<String?>(null) }
        val isCorrectAnswerSelected = selectedOption?.let { it == state.question.answer }


        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {

            LinearProgressComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                progress = progress.value,
                countProgress = "${state.indexQuestion}/${state.questions.size}"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "${state.question.id}.- ${state.question.title}",
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


            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                val options = state.question.options

                itemsIndexed(options) { index, option ->
                    AnswerCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = option,
                        isCorrect = state.question.validationAnswer(index),
                        isSelected = selectedOption == option,
                        isCorrectAnswerSelected = isCorrectAnswerSelected,
                        onClick = {
                            selectedOption = option

                            if (selectedOption.isNullOrEmpty()) {

                            }

                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonsAction(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                onClickNextQuestion = {
                    onAction(EvaluationScreenAction.NextQuestion)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}


@Composable
fun AnswerCard(
    text: String,
    isCorrect: Boolean,
    isSelected: Boolean,
    isCorrectAnswerSelected: Boolean?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val backgroundColor = when {
        isSelected && isCorrect -> Color(0xFFC8E6C9) // Verde si es la correcta
        isSelected -> Color(0xFFFFCDD2) // Rojo si es incorrecta
        isCorrectAnswerSelected == false && isCorrect -> Color(0xFFC8E6C9) // Muestra la correcta si falló
        else -> Color.White
    }

    val borderColor = when {
        isSelected && isCorrect -> Color(0xFF388E3C)
        isSelected -> Color(0xFFD32F2F)
        isCorrectAnswerSelected == false && isCorrect -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(
        modifier = modifier
            .clickable(enabled = isCorrectAnswerSelected == null) {
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
    onClickNextQuestion: () -> Unit = {},
) {

    var selectedOption = if (state.isVerifyAnswer) stringResource(R.string.verify) else stringResource(R.string.next)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            enabled = state.answerWasSelected,
            onClick = {
                onClickNextQuestion()
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = selectedOption)
        }

    }
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
                question = question
            ),
            onAction = {}
        )
    }
}