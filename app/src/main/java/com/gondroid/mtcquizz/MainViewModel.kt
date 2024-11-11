package com.gondroid.mtcquizz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainViewModel(firebaseInstance: FirebaseInstance) : ViewModel() {

    private val db = firebaseInstance

    private val _data = MutableStateFlow<String?>(null)

    val data: StateFlow<String?> = _data

    init {
        getRealTimeDatabase()
    }

    private fun getRealTimeDatabase() {
        viewModelScope.launch {
            collectDatabaseReference().collect {
                val data = it.getValue(String::class.java)
                _data.value = data
                Log.i("SuccessDB", "${data}")

            }

        }
    }

    fun writeOnFirebase(value: String) {
        db.writeOnFirebase(value)
    }

    private fun collectDatabaseReference(): Flow<DataSnapshot> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("SuccessDB", "onDataChange")
                trySend(snapshot).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "onCancelled ${error.details}")
                close(error.toException())
            }
        }
        db.addEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }
}