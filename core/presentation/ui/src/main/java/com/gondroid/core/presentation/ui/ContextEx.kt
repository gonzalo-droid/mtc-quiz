package com.gondroid.core.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The [Activity] behind a Compose `LocalContext`, which may be wrapped (themes, Hilt). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
