package com.gondroid.mtcquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.gondroid.core.data.ads.AdsConsentManager
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.ui.LocalCanRequestAds
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var adsConsentManager: AdsConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        // UMP first: the Mobile Ads SDK only starts once consent allows ad requests.
        adsConsentManager.gatherConsent(this)
        setContent {
            var keepSplashScreen by remember { mutableStateOf(true) }
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
            val authState by viewModel.state.collectAsState()
            val canRequestAds by adsConsentManager.canRequestAds.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            MTCQuizTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalCanRequestAds provides canRequestAds) {
                    if (!authState.isLoading) {
                        keepSplashScreen = false
                        val navController = rememberNavController()
                        NavigationRoot(
                            navController,
                            authState.isLoggedIn,
                            authState.isOnboardingShown
                        )
                    }
                }
            }
        }
    }
}
