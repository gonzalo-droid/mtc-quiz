package com.gondroid.mtcquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        setContent {
            var keepSplashScreen by remember { mutableStateOf(true) }
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
            val authState by viewModel.state.collectAsState()

            MTCQuizTheme {
                if (!authState.isLoading) {
                    keepSplashScreen = false
                    val navController = rememberNavController()
                    NavigationRoot(navController, authState.isLoggedIn)
                }
            }
        }
    }
}



