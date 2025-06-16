package com.gondroid.detail.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.presentation.screens.detail.providers.DetailScreenPreviewProvider
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import com.gondroid.presentation.screens.detail.providers.DetailScreenPreviewProvider

@Composable
fun DetailScreenRoot(
    viewModel: DetailScreenViewModel,
    navigateBack: () -> Boolean,
    navigateToConfiguration: () -> Unit,
    navigateToQuestions: (String) -> Unit,
    navigateToShowPDF: (String) -> Unit,
    navigateToEvaluation: (String) -> Unit,
) {

    val state by viewModel.state.collectAsState()


    DetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is DetailAction.Back -> navigateBack()
                is DetailAction.GoToConfiguration -> navigateToConfiguration()
                is DetailAction.GoToEvaluation -> navigateToEvaluation(action.categoryId)
                is DetailAction.GoToQuestions -> navigateToQuestions(action.categoryId)
                is DetailAction.ShowPDF -> navigateToShowPDF(action.categoryId)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailState,
    onAction: (DetailAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().semantics{
            contentDescription = "detail_screen"
        },
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
                                    DetailAction.Back,
                                )
                            }.semantics{
                                contentDescription = "back_button"
                            },
                    )
                },
                actions = {
                    Box(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                    onAction(DetailAction.GoToConfiguration)
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
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                modifier = Modifier,
                text = "* Licencia de conducir para conductores no profesionales",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )


            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(
                onGoToEvaluation = { onAction(DetailAction.GoToEvaluation(state.category.id)) },
                onGoToQuestions = { onAction(DetailAction.GoToQuestions(state.category.id)) },
                onShowPdf = { onAction(DetailAction.ShowPDF(state.category.id)) }
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
                .semantics{
                contentDescription = "start_evaluation"
            }
        ) {
            Text(text = stringResource(R.string.start_evaluation))
        }

        OutlinedButton(
            onClick = onGoToQuestions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics{
                    contentDescription = "study"
                },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "PlayCircle",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.study))
        }

        TextButton(
            onClick = onShowPdf,
            modifier = Modifier.fillMaxWidth()
                .semantics{
                    contentDescription = "download_pdf"
                },
        ) {
            Text(text = stringResource(R.string.download_pdf))
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
    @PreviewParameter(DetailScreenPreviewProvider::class) state: DetailState,
) {
    MTCQuizTheme {
        DetailScreen(
            state = state,
            onAction = {}
        )
    }
}
