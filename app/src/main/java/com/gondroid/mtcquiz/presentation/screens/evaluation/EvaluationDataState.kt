package com.gondroid.mtcquiz.presentation.screens.evaluation

import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Question

data class EvaluationDataState(
    val questions: List<Question> = emptyList(),
    val question: Question = Question(),
    val indexQuestion: Int = 0,
    val answerWasSelected: Boolean = false,
    val answerWasVerified: Boolean = false,
    val isFinishExam: Boolean = false,
    val category: Category = Category(),

    // summary
    val date: String = "",
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val totalMinutes : Int = 1
)