package com.gondroid.questionreview.presentation

import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Question

data class QuestionsState(
    val questions: List<Question> = emptyList(),
    val category: Category = Category()
)