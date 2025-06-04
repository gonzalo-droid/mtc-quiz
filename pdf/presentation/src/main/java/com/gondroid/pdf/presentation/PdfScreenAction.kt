package com.gondroid.pdf.presentation

sealed interface PdfScreenAction {
    data object Back : PdfScreenAction
    data object Downloading : PdfScreenAction
}