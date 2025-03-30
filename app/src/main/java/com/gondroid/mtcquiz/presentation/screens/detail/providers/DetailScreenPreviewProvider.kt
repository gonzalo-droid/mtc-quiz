package com.gondroid.mtcquiz.presentation.screens.detail.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource
import com.gondroid.mtcquiz.presentation.screens.detail.DetailDataState

class DetailScreenPreviewProvider : PreviewParameterProvider<DetailDataState> {
    override val values: Sequence<DetailDataState>
        get() = sequenceOf(
            DetailDataState(
                category = categoriesLocalDataSource.first()
            )
        )

}
