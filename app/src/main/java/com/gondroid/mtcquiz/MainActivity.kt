package com.gondroid.mtcquizz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.gondroid.mtcquizz.presentation.navigation.NavigationRoot
import com.gondroid.mtcquizz.ui.theme.MTCQuizzTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var firebaseInstance: FirebaseInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //firebaseInstance = FirebaseInstance(this)
        // val viewModel = MainViewModel(firebaseInstance)

        enableEdgeToEdge()
        setContent {
            MTCQuizzTheme {
                val navController = rememberNavController()
                NavigationRoot(navController)
            }
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    val list by viewModel.list.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hola Mundo",
            modifier = modifier.padding(bottom = 4.dp),
            fontSize = 20.sp,
        )
        Divider()

        ListItems(
            todoList = list ?: emptyList(),
            modifier = modifier.weight(1f),
            onItemSelected = { type, key ->
                when (type) {
                    Actions.DELETE -> {
                        viewModel.removedItem(key)
                    }

                    else -> {
                        viewModel.updateItem(key)
                    }
                }
            })

        Button(
            onClick = { writeOnFirebase(viewModel) },
        ) {
            Text(text = "Update", fontSize = 16.sp)
        }
    }
}

@Composable
fun ListItems(
    todoList: List<Pair<String, Todo>>,
    modifier: Modifier,
    onItemSelected: (Actions, String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .background(color = Color.White)
    ) {
        items(todoList) { (key, todo) ->
            TodoItem(modifier = Modifier, key = key, todo = todo, onItemSelected = { type, key ->
                onItemSelected(type, key)
            })
        }
    }
}


@Composable
fun TodoItem(
    key: String, todo: Todo, modifier: Modifier.Companion, onItemSelected: (Actions, String) -> Unit
) {

    val imageVector = if (todo.done!!) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .background(Color.LightGray)
            .padding(16.dp)
    ) {
        Column(
        ) {
            Text(
                text = todo.title!!,
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Text(
                text = todo.description!!,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 14.sp
            )

            Text(
                text = if (todo.done!!) "Completed" else "Pending",
                color = if (todo.done!!) Color.Green else Color.Red,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(modifier.weight(1f))

        Column {
            IconButton(
                onClick = { onItemSelected(Actions.DONE, key) },
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = "Done",
                    tint = Color.Magenta // Cambia el color si necesitas otro
                )
            }

            Spacer(modifier.weight(1f))

            IconButton(
                onClick = { onItemSelected(Actions.DELETE, key) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red // Cambia el color si necesitas otro
                )
            }
        }
    }
}


fun writeOnFirebase(viewModel: MainViewModel) {
    val time = System.currentTimeMillis()
    viewModel.writeOnFirebase(time.toString())
}


enum class Actions {
    DELETE, DONE
}
