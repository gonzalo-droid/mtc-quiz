package com.gondroid.detail.presentation.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource
import com.gondroid.presentation.screens.detail.DetailDataState

class DetailScreenPreviewProvider : PreviewParameterProvider<DetailDataState> {
    override val values: Sequence<DetailDataState>
        get() = sequenceOf(
            DetailDataState(
                category = categoriesLocalDataSource.first()
            )
        )

}
