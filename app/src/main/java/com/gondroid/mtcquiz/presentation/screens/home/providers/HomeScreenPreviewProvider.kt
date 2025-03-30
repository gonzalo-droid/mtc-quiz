package com.gondroid.mtcquiz.presentation.screens.home.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource
import com.gondroid.mtcquiz.presentation.screens.home.HomeDataState

class HomeScreenPreviewProvider : PreviewParameterProvider<HomeDataState> {

    override val values: Sequence<HomeDataState>
        get() =
            sequenceOf(
                HomeDataState(
                    categories = categoriesLocalDataSource
                )
            )
}
