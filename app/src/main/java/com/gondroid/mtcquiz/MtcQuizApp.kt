package com.gondroid.mtcquiz

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MtcQuizApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        // Plant appropriate Timber tree based on build type
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
    }

    private class CrashReportingTree : Timber.Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?
        ) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            val exception = t ?: Exception(message)

            // Crashlytics
            val crashlytics = Firebase.crashlytics
            crashlytics.setCustomKey(KEY_PRIORITY, priority)
            crashlytics.setCustomKey(KEY_TAG, tag.orEmpty())
            crashlytics.setCustomKey(KEY_MESSAGE, message)

            // Firebase Crash Reporting
            crashlytics.log("$priority $tag $message")
            crashlytics.recordException(exception)
        }

        companion object {
            private const val KEY_PRIORITY = "priority"
            private const val KEY_TAG = "tag"
            private const val KEY_MESSAGE = "message"
        }
    }
}