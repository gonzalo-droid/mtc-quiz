package com.gondroid.mtcquizz.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gondroid.mtcquizz.presentation.screens.home.HomeScreenRoot
import com.gondroid.mtcquizz.presentation.screens.home.HomeScreenViewModel

@Composable
fun NavigationRoot(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeScreenRoute,
        ) {

            composable<HomeScreenRoute> {
                val viewmodel = hiltViewModel<HomeScreenViewModel>()
                HomeScreenRoot(
                    viewModel = viewmodel,
                    navigateTo = {
                        navController.navigateUp()
                    },
                )
            }

        }
    }
}