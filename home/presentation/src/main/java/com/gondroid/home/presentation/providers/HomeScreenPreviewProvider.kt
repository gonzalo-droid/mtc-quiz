package com.gondroid.home.presentation.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gondroid.home.presentation.HomeState
import com.gondroid.mtcquiz.data.local.quiz.categoriesLocalDataSource

class HomeScreenPreviewProvider : PreviewParameterProvider<HomeState> {

    override val values: Sequence<HomeState>
        get() =
            sequenceOf(
                HomeState(
                    categories = categoriesLocalDataSource
                )
            )
}
