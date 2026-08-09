package com.gondroid.core.domain.model

data class Category(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val classType: String = "",
    val description: String = "",
    val pdf: String = "",
    val pathJson: String = ""
) {
    val examId: String
        get() = pathJson.removeSuffix("_questions.json")
}
