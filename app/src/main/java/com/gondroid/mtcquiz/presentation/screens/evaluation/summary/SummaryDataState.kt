package com.gondroid.mtcquiz.presentation.screens.evaluation.summary

import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Evaluation
import com.gondroid.mtcquiz.domain.models.Question

data class SummaryDataState(
    val date: String = "",
    val category: Category = Category(),
    val evaluation: Evaluation = Evaluation(),
)