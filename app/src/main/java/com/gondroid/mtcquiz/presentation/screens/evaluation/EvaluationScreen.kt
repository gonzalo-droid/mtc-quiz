package com.gondroid.mtcquiz.presentation.screens.evaluation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.domain.models.Answer
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
    var currentQuestion by remember { mutableStateOf(33) }
    val totalQuestions = 40
    // Calcula el progreso como un valor float entre 0 y 1.
    val progress = currentQuestion / totalQuestions.toFloat()

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

        Column(
            modifier =
            Modifier
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .height(8.dp)
                        .weight(1f),
                )
                Text(
                    text = "1/40",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Respecto de los dispositivos de control o regulación del tránsito:",
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
                modifier = Modifier.weight(1f), // Para empujar los items hacia abajo
                verticalArrangement = Arrangement.Bottom
            ) {

                val answers = listOf(
                    Answer("1", "a) Recoger o dejar pasajeros o carga en cualquier lugar"),
                    Answer(
                        "2",
                        "Dejar animales sueltos o situarlos de forma tal que obstaculicen solo un poco el tránsito"
                    ),
                    Answer("3", "Recoger o dejar pasajeros en lugares autorizados."),
                    Answer("4", "Ejercer el comercio ambulatorio o estacionario"),
                )

                items(
                    items = answers,
                    key = { answer -> answer.id }
                ) { answer ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AnswerCard(
                        text = answer.title,
                        isCorrectAnswer = answer.id == "3",
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ButtonsAction(
                modifier =
                Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}


@Composable
fun AnswerCard(
    text: String,
    isCorrectAnswer: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Estado para determinar si ya se hizo clic en la tarjeta
    var clicked by remember { mutableStateOf(false) }

    // Colores que varían según si se hizo clic y la corrección de la respuesta
    val backgroundColor = if (clicked) {
        if (isCorrectAnswer) Color(0xFFC8E6C9) // verde claro
        else Color(0xFFFFCDD2) // rojo claro
    } else Color.White

    val borderColor = if (clicked) {
        if (isCorrectAnswer) Color(0xFF388E3C) // verde oscuro
        else Color(0xFFD32F2F) // rojo oscuro
    } else Color.Gray

    Card(
        modifier = modifier
            .clickable {
                clicked = true
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
    modifier: Modifier,
    onClickAds: () -> Unit = {},
    onDownloadPdf: () -> Unit = {},
) {


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onClickAds,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FullscreenExit,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Siguiente")
        }

    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewEvaluationScreenRoot() {
    MTCQuizTheme {
        EvaluationScreen(
            state = EvaluationDataState(),
            onAction = {}
        )
    }
}