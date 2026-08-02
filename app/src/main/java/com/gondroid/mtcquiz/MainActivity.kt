package com.gondroid.mtcquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
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
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            MTCQuizTheme(darkTheme = isDark) {
                if (!authState.isLoading) {
                    keepSplashScreen = false
                    val navController = rememberNavController()
                    NavigationRoot(navController, authState.isLoggedIn, authState.isOnboardingShown)
                }
            }
        }
    }
}



