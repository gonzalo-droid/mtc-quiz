package com.gondroid.mtcquizz

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class FirebaseInstance(context: Context) {
    private val database = Firebase.database

    init {
        FirebaseApp.initializeApp(context)
    }

    fun writeOnFirebase(value: String) {
        database.reference.setValue(value)
    }

    fun addEventListener(postListener: ValueEventListener) {
        database.reference.addValueEventListener(postListener)
    }

    fun removeEventListener(postListener: ValueEventListener) {
        database.reference.removeEventListener(postListener)
    }
}