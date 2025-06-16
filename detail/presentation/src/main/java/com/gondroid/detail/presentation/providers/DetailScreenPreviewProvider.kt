package com.gondroid.detail.presentation.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.detail.presentation.DetailState
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource
import com.gondroid.presentation.screens.detail.DetailDataState

class DetailScreenPreviewProvider : PreviewParameterProvider<DetailState> {
    override val values: Sequence<DetailState>
        get() = sequenceOf(
            DetailState(
                category = categoriesLocalDataSource.first()
            )
        )

}
