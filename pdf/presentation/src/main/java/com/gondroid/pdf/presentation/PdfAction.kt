package com.gondroid.pdf.presentation

sealed interface PdfAction {
    data object Back : PdfAction
    data object Downloading : PdfAction
}