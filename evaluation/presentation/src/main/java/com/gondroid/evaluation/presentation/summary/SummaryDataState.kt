package com.gondroid.evaluation.presentation.summary

import com.gondroid.domain.models.Category
import com.gondroid.domain.models.Evaluation

data class SummaryDataState(
    val date: String = "",
    val category: Category = Category(),
    val evaluation: Evaluation = Evaluation(),
)