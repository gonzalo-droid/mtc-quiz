package com.gondroid.mtcquiz.presentation.screens.sendComment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.presentation.screens.sendComment.FirebaseInstance
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class SendCommentViewModel(firebaseInstance: FirebaseInstance) : ViewModel() {

    private val db = firebaseInstance

    private val _data = MutableStateFlow<String?>(null)

    val data: StateFlow<String?> = _data

    private val _list = MutableStateFlow<List<Pair<String, Todo>>?>(null)

    val list: StateFlow<List<Pair<String, Todo>>?> = _list

    init {
        getRealTimeDatabase()
    }

    private fun getRealTimeDatabase() {
        viewModelScope.launch {
            collectDatabaseReference().collect {
                val data = getCleanSnapshot(it)
                _list.value = data
            }
        }
    }

    private fun getCleanSnapshot(snapshot: DataSnapshot): List<Pair<String, Todo>> {
        val list = snapshot.children.map { item ->
            Pair(item.key.toString(), item.getValue(Todo::class.java)!!)
        }
        return list
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

    fun removedItem(value: String) {
        db.removedItem(value)
    }

    fun updateItem(value: String) {
        db.updateItem(value)

    }
}