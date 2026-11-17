package com.gondroid.questionreview.presentation

import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Question

data class QuestionsState(
    val questions: List<Question> = emptyList(),
    val category: Category = Category(),
    val isPremium: Boolean = false,
    /** Cada cuántas preguntas va un banner (Remote Config `study_banner_interval`); 0 = ninguno. */
    val bannerInterval: Int = 0
) {
    /** Premium nunca ve anuncios; si no, manda el intervalo remoto. */
    val effectiveBannerInterval: Int get() = if (isPremium) 0 else bannerInterval
}
