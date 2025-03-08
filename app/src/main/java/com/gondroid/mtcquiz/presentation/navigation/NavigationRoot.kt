package com.gondroid.mtcquiz.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationScreenRoot
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenRoot
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenRoot
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenRoot
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModel

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
                    navigateTo = { categoryId ->
                        navController.navigate(
                            DetailScreenRoute(
                                categoryId = categoryId
                            )
                        )
                    },
                )
            }

            composable<DetailScreenRoute> {
                val viewModel = hiltViewModel<DetailScreenViewModel>()
                DetailScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEvaluation = {
                        navController.navigate(
                            EvaluationScreenRoute
                        )
                    },
                    navigateToAllQuestions = {
                        navController.navigate(
                            EvaluationScreenRoute
                        )
                    },
                    navigateToShowPDF = {

                    },
                    navigateToConfiguration = {
                        navController.navigate(
                            ConfigurationScreenRoute
                        )
                    }
                )
            }

            composable<EvaluationScreenRoute> {
                val viewModel = hiltViewModel<EvaluationScreenViewModel>()
                EvaluationScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<ConfigurationScreenRoute> {
                val viewModel = hiltViewModel<ConfigurationScreenViewModel>()
                ConfigurationScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

        }
    }
}