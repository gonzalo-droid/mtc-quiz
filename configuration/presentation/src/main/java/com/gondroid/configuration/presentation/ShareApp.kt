package com.gondroid.configuration.presentation

import android.content.Context
import android.content.Intent

class ShareApp {

    operator fun invoke(context: Context) {
        val playStoreLink = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Prepárate para tu examen de manejo con MTCQuiz: $playStoreLink")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartir MTCQuiz"))
    }
}
