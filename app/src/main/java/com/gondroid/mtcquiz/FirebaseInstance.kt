package com.gondroid.mtcquizz

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class FirebaseInstance(context: Context) {
    private val database = Firebase.database
    private val myRef = database.reference

    init {
        FirebaseApp.initializeApp(context)
    }

    fun writeOnFirebase(value: String) {
        val newNode = myRef.push()
        newNode.setValue(getGenericTodoTaskItem(value))
    }

    fun addEventListener(postListener: ValueEventListener) {
        myRef.addValueEventListener(postListener)
    }

    fun removeEventListener(postListener: ValueEventListener) {
        myRef.removeEventListener(postListener)
    }

    private fun getGenericTodoTaskItem(randomValue: String): Todo {
        return Todo(
            title = "Time $randomValue",
            description = "description"
        )
    }

    fun removedItem(reference: String) {
        myRef.child(reference).removeValue()
    }

    fun updateItem(reference: String) {
        // TODO: firebase change attribute name, why? IDW
        myRef.child(reference).child("done").setValue(true)
    }
}