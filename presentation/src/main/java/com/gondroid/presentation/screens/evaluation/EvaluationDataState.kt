package com.gondroid.presentation.screens.evaluation

import com.gondroid.domain.models.Category
import com.gondroid.domain.models.Question

data class EvaluationDataState(
    val questions: List<Question> = emptyList(),
    val question: Question = Question(),
    val indexQuestion: Int = 0,
    val answerWasSelected: Boolean = false,
    val answerWasVerified: Boolean = false,
    val isFinishExam: Boolean = false,
    val category: Category = Category(),
    val totalMinutes : Int = 0
)