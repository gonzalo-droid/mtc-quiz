package com.gondroid.mtcquiz

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MTCApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MTCApp", applicationContext.packageName)
    }
}