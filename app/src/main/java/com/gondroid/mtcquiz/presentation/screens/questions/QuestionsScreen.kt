package com.gondroid.mtcquiz.presentation.screens.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.domain.models.Question
import com.gondroid.mtcquiz.presentation.component.LinearProgressComponent
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme


@Composable
fun QuestionsScreenRoot(
    viewModel: QuestionsScreenViewModel,
    navigateBack: () -> Boolean,
) {
    val state = viewModel.state
    QuestionsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                QuestionsScreenAction.Back -> navigateBack()
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    state: QuestionsDataState,
    onAction: (QuestionsScreenAction) -> Unit,
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val filteredItems = state.questions.filter { it.title.contains(searchText, ignoreCase = true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            textStyle = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            placeholder = { Text(stringResource(R.string.searching)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchExpanded = false
                                    searchText = ""
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.close_searching)
                                    )
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "CLASE A - CATEGORÍA I",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                    }


                },
                navigationIcon = {
                    if (!isSearchExpanded) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                Modifier.clickable {
                                    onAction(
                                        QuestionsScreenAction.Back,
                                    )
                                },
                        )
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->

        val scrollState = rememberLazyListState()
        var startItemVisible by remember { mutableIntStateOf(1) }
        val progress = remember {
            derivedStateOf {
                if (scrollState.layoutInfo.totalItemsCount > 0) {
                    startItemVisible = scrollState.firstVisibleItemIndex.toInt() + 2
                    scrollState.firstVisibleItemIndex.toFloat() / (scrollState.layoutInfo.totalItemsCount - 1)
                } else {
                    0f
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {

            if (!isSearchExpanded) {
                LinearProgressComponent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    progress = progress.value,
                    countProgress = "${startItemVisible}/${state.questions.size}"
                )
            }


            if (filteredItems.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = "SearchOff",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )
                Text(
                    text = "Sin resultados encontrados",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleSmall.fontSize
                )

            } else {
                LazyColumn(
                    state = scrollState, modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredItems,
                        key = { questions -> questions.id }
                    ) { question ->
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

                        Spacer(modifier = Modifier.height(8.dp))

                        question.options.forEachIndexed { index, option ->
                            ItemAnswerCard(
                                text = option,
                                isCorrectAnswer = question.validationAnswer(index),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                }
            }


        }
    }
}


@Composable
fun ItemAnswerCard(
    text: String,
    isCorrectAnswer: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCorrectAnswer) Color(0xFFC8E6C9) else Color.White

    val borderColor = if (isCorrectAnswer) Color(0xFF388E3C) else Color.Gray

    Card(
        modifier = modifier,
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


@Preview(
    showBackground = true,
)
@Composable
fun PreviewQuestionsScreenRoot() {
    val answers = listOf(
        "a) Recoger o dejar pasajeros o carga en cualquier lugar",
        "b) Recoger o dejar pasajeros o carga en cualquier lugar",
        "c) Recoger o dejar pasajeros o carga en cualquier lugar",
        "d) Recoger o dejar pasajeros o carga en cualquier lugar"
    )

    val questions = listOf(
        Question(
            id = 1,
            title = "Respecto de los 100 de control o regulación del tránsito:",
            topic = "",
            section = "",
            options = answers,
            answer = "a",
        ),
        Question(
            id = 2,
            title = "Respecto de los 100 de control o regulación del tránsito:",
            topic = "",
            section = "",
            options = answers,
            answer = "b",
        ),
        Question(
            id = 3,
            title = "Respecto de los 100 de control o regulación del tránsito:",
            topic = "",
            section = "",
            options = answers,
            answer = "c",
        ),
    )

    MTCQuizTheme {
        QuestionsScreen(
            state = QuestionsDataState(questions = questions),
            onAction = {}
        )
    }
}