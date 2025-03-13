package com.gondroid.mtcquiz.presentation.screens.pdf

sealed interface PdfScreenAction {
    data object Back : PdfScreenAction
}