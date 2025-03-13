package com.gondroid.mtcquiz.presentation.screens.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.presentation.screens.detail.providers.DetailScreenPreviewProvider
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@Composable
fun DetailScreenRoot(
    viewModel: DetailScreenViewModel,
    navigateBack: () -> Boolean,
    navigateToConfiguration: () -> Unit,
    navigateToQuestions: () -> Unit,
    navigateToShowPDF: () -> Unit,
    navigateToEvaluation: () -> Unit,
) {

    val state = viewModel.state

    DetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                DetailScreenAction.Back -> navigateBack()
                DetailScreenAction.GoToEvaluation -> navigateToEvaluation()
                DetailScreenAction.ShowPDF -> navigateToShowPDF()
                DetailScreenAction.GoToQuestions -> navigateToQuestions()
                DetailScreenAction.GoToConfiguration -> navigateToConfiguration()
            }

        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailDataState,
    onAction: (DetailScreenAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
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
                                    DetailScreenAction.Back,
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
                                    onAction(DetailScreenAction.GoToConfiguration)
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onSurface,
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
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = state.category.category,
                        modifier = Modifier,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        modifier = Modifier,
                        text = state.category.title,
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

            Text(
                modifier = Modifier,
                text = state.category.description,
                fontSize = 15.sp,

                )
            Text(
                modifier = Modifier,
                text = "* Licencia de conducir para conductores no profesionales",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,

                )


            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(
                onGoToEvaluation = { onAction(DetailScreenAction.GoToEvaluation) },
                onGoToQuestions = { onAction(DetailScreenAction.GoToQuestions) },
                onShowPdf = { onAction(DetailScreenAction.ShowPDF) }
            )
        }

    }
}

@Composable
fun ButtonsAction(
    onGoToEvaluation: () -> Unit = {},
    onGoToQuestions: () -> Unit = {},
    onShowPdf: () -> Unit = {},
) {

    // Creamos una transición infinita para animar el botón
    val infiniteTransition = rememberInfiniteTransition()
    // Animamos la escala para que varíe entre 0.95 y 1.05, generando un efecto de "latido"
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGoToEvaluation,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)

        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Simulacro")
        }

        OutlinedButton(
            onClick = onGoToQuestions,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Estudiar")
        }
        // Botón secundario
        TextButton(
            onClick = onShowPdf,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Descargar PDF")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "PlayCircle",
            )
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewDetailScreenRoot(
    @PreviewParameter(DetailScreenPreviewProvider::class) state: DetailDataState,
) {
    MTCQuizTheme {
        DetailScreen(
            state = state,
            onAction = {}
        )
    }
}
