package com.gondroid.mtcquizz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.mtcquizz.ui.theme.MTCQuizzTheme

class MainActivity : ComponentActivity() {

    private lateinit var firebaseInstance: FirebaseInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseInstance = FirebaseInstance(this)

        enableEdgeToEdge()

        val viewModel = MainViewModel(firebaseInstance)

        setContent {
            MTCQuizzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    val data by viewModel.data.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$data",
            modifier = modifier.padding(16.dp),
            fontSize = 20.sp,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { writeOnFirebase(viewModel) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Update")
        }
    }
}

fun writeOnFirebase(viewModel: MainViewModel) {
    val time = System.currentTimeMillis()
    viewModel.writeOnFirebase(time.toString())
}

