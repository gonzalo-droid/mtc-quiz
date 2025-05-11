package com.gondroid.mtcquiz.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class OpenAppInPlayStore {

    operator fun invoke(context: Context) {
        val packageName: String = context.packageName
        val uri = "market://details?id=$packageName".toUri()
        val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        try {
            context.startActivity(goToMarketIntent)
        } catch (e: ActivityNotFoundException) {
            val webUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }
}