package com.gondroid.evaluation.presentation.summary

import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Evaluation

data class SummaryState(
    val date: String = "",
    val category: Category = Category(),
    val evaluation: Evaluation = Evaluation(),
)