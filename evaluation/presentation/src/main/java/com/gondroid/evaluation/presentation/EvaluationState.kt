package com.gondroid.evaluation.presentation

import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Question

data class EvaluationState(
    val questions: List<Question> = emptyList(),
    val question: Question = Question(),
    val indexQuestion: Int = 0,
    val answerWasSelected: Boolean = false,
    val answerWasVerified: Boolean = false,
    val isFinishExam: Boolean = false,
    val category: Category = Category(),
    val totalMinutes : Int = 0
)