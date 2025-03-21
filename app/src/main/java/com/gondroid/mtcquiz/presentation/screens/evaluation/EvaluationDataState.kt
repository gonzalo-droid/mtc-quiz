package com.gondroid.mtcquiz.presentation.screens.evaluation

import com.gondroid.mtcquiz.domain.models.Question

data class EvaluationDataState(
    val questions: List<Question> = emptyList(),
    val question: Question = Question(),
    val indexQuestion: Int = 0
)