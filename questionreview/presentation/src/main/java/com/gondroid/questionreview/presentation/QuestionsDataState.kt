package com.gondroid.questionreview.presentation

import com.gondroid.domain.models.Category
import com.gondroid.domain.models.Question

data class QuestionsDataState(
    val questions: List<Question> = emptyList(),
    val category: Category = Category()
)