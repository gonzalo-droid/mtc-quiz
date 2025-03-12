package com.gondroid.mtcquiz.presentation.screens.detail.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.mtcquiz.data.local.quiz.repository.categoriesLocalDataSource
import com.gondroid.mtcquiz.presentation.screens.detail.DetailDataState
import com.gondroid.mtcquiz.presentation.screens.home.HomeDataState

class DetailScreenPreviewProvider : PreviewParameterProvider<DetailDataState> {
    override val values: Sequence<DetailDataState>
        get() = sequenceOf(
            DetailDataState(
                category = categoriesLocalDataSource.first()
            )
        )

}
