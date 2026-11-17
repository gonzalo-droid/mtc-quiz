package com.gondroid.questionreview.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.core.domain.model.Question
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.components.AnswerOption
import com.gondroid.core.presentation.designsystem.components.AnswerOptionState
import com.gondroid.core.presentation.designsystem.components.LinearProgressComponent
import com.gondroid.core.presentation.designsystem.components.QuestionAnswerCard
import com.gondroid.core.presentation.ui.InlineBannerAdSlot
import com.gondroid.core.presentation.ui.normalizeText
import com.gondroid.core.presentation.ui.rememberInlineBannerAds
import com.gondroid.core.presentation.ui.stripOptionLetterPrefix

@Composable
fun QuestionsScreenRoot(viewModel: QuestionsScreenViewModel, navigateBack: () -> Boolean) {
    val state by viewModel.state.collectAsState()

    QuestionsScreen(
        state = state,
        bannerAdId = viewModel.bannerAdId,
        onAction = { action ->
            when (action) {
                QuestionsAction.Back -> navigateBack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    state: QuestionsState,
    bannerAdId: String,
    onAction: (QuestionsAction) -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    // Keep each question's position in the full bank: it is the LazyColumn key (see
    // StudyListEntry.QuestionEntry for why question.id cannot be).
    val filteredItems = state.questions.withIndex().filter { (_, question) ->
        question.title.normalizeText().contains(searchText.normalizeText(), ignoreCase = true)
    }
    // No banners while searching: the results are a short list the user wants to scan at once.
    val bannerInterval = if (searchText.isBlank()) state.effectiveBannerInterval else 0
    val entries = filteredItems.withBanners(bannerInterval)
    val questionsBefore by rememberUpdatedState(entries.questionsBefore())
    val questionCount by rememberUpdatedState(filteredItems.size)
    // The list has 16.dp of padding on each side.
    val banners = rememberInlineBannerAds(bannerAdId, horizontalInset = 32.dp)

    val scrollState = rememberLazyListState()
    var startItemVisible by remember { mutableIntStateOf(1) }
    // Progress counts questions, not list rows: the banners in between would inflate it.
    val progress by remember {
        derivedStateOf {
            val firstVisibleItem = scrollState.firstVisibleItemIndex
            val lastVisibleItem =
                scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val before = questionsBefore

            if (questionCount > 1) {
                startItemVisible = before.getOrElse(firstVisibleItem) { firstVisibleItem } + 2
                val lastQuestion = before.getOrElse(lastVisibleItem) { lastVisibleItem }
                val normalizedProgress =
                    lastQuestion.toFloat() / (questionCount - 1).coerceAtLeast(1)
                normalizedProgress.coerceIn(0f, 1f)
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
                                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchExpanded = false
                                    searchText = ""
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(
                                            R.string.close_searching
                                        )
                                    )
                                }
                            }
                        )
                    } else {
                        Text(
                            text = state.category.title,
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
                                    QuestionsAction.Back
                                )
                            }
                        )
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (!isSearchExpanded) {
                LinearProgressComponent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    progress = progress,
                    countProgress = "$startItemVisible/${state.questions.size}"
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
                    state = scrollState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = entries,
                        key = { entry -> entry.key },
                        contentType = { entry -> entry::class }
                    ) { entry ->
                        when (entry) {
                            is StudyListEntry.BannerEntry ->
                                InlineBannerAdSlot(banners = banners, slot = entry.slot)
                            is StudyListEntry.QuestionEntry -> {
                                StudyQuestionCard(entry.question)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyQuestionCard(question: Question) {
    QuestionAnswerCard(
        modifier = Modifier.fillMaxWidth(),
        title = "${question.id}.- ${question.title}",
        questionImages = question.imagens,
        options = question.options.mapIndexed { index, option ->
            val letter = ('a' + index).uppercaseChar().toString()
            val optionState = if (question.isCorrectAnswer(index)) {
                AnswerOptionState.RevealedCorrect
            } else {
                AnswerOptionState.Unselected
            }
            AnswerOption(
                letter = letter,
                text = option.stripOptionLetterPrefix(),
                state = optionState
            )
        }
    )
}

@Preview(
    showBackground = true
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
            answer = "a"
        ),
        Question(
            id = 2,
            title = "Respecto de los 100 de control o regulación del tránsito:",
            topic = "",
            section = "",
            options = answers,
            answer = "b"
        ),
        Question(
            id = 3,
            title = "Respecto de los 100 de control o regulación del tránsito:",
            topic = "",
            section = "",
            options = answers,
            answer = "c"
        )
    )

    MTCQuizTheme {
        QuestionsScreen(
            state = QuestionsState(questions = questions),
            bannerAdId = "test-banner-id",
            onAction = {}
        )
    }
}
