package com.gondroid.mtcquiz.presentation.screens.questions

import com.gondroid.mtcquiz.domain.models.Question

data class QuestionsDataState(
    val questions: List<Question> = emptyList()
)