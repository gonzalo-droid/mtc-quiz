package com.gondroid.home.presentation.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.home.presentation.HomeDataState
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource

class HomeScreenPreviewProvider : PreviewParameterProvider<HomeDataState> {

    override val values: Sequence<HomeDataState>
        get() =
            sequenceOf(
                HomeDataState(
                    categories = categoriesLocalDataSource
                )
            )
}
