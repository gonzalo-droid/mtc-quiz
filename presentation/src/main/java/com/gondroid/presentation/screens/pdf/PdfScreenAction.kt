package com.gondroid.presentation.screens.pdf

sealed interface PdfScreenAction {
    data object Back : PdfScreenAction
    data object Downloading : PdfScreenAction
}