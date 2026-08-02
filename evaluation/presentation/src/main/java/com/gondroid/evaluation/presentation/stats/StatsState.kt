package com.gondroid.evaluation.presentation.stats

data class StatsState(
    val totalEvaluations: Int = 0,
    val totalApproved: Int = 0,
    val totalRejected: Int = 0,
    val approvalRate: Float = 0f,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true,
)

data class CategoryStat(
    val categoryTitle: String,
    val evaluationCount: Int,
    val approvalRate: Float,
)
