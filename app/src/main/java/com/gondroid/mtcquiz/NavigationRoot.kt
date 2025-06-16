package com.gondroid.mtcquiz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gondroid.auth.presentation.LoginScreenRoot
import com.gondroid.auth.presentation.LoginScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationScreenRoot
import com.gondroid.mtcquiz.presentation.screens.configuration.ConfigurationScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.configuration.customize.CustomizeScreenRoot
import com.gondroid.mtcquiz.presentation.screens.configuration.customize.CustomizeScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.configuration.term.TermScreenRoot
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenRoot
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenRoot
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.evaluation.summary.SummaryScreenRoot
import com.gondroid.mtcquiz.presentation.screens.evaluation.summary.SummaryScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenRoot
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.pdf.PdfScreenRoot
import com.gondroid.mtcquiz.presentation.screens.pdf.PdfScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.questions.QuestionsScreenRoot
import com.gondroid.mtcquiz.presentation.screens.questions.QuestionsScreenViewModel

@Composable
fun NavigationRoot(navController: NavHostController, isLoggedIn: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) HomeScreenRoute else LoginScreenRoute,
        ) {

            composable<HomeScreenRoute> {
                val viewmodel = hiltViewModel<HomeScreenViewModel>()
                HomeScreenRoot(
                    viewModel = viewmodel,
                    navigateToDetail = { categoryId ->
                        navController.navigate(
                            DetailScreenRoute(
                                categoryId = categoryId
                            )
                        )
                    },
                    navigateToConfiguration = {
                        navController.navigate(
                            ConfigurationScreenRoute
                        )
                    }
                )
            }

            composable<DetailScreenRoute> {
                val viewModel = hiltViewModel<DetailScreenViewModel>()
                DetailScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEvaluation = { categoryId ->
                        navController.navigate(
                            EvaluationScreenRoute(
                                categoryId = categoryId
                            )
                        )
                    },
                    navigateToQuestions = { categoryId ->
                        navController.navigate(
                            QuestionsScreenRoute(
                                categoryId = categoryId
                            )
                        )
                    },
                    navigateToShowPDF = { categoryId ->
                        navController.navigate(
                            PdfScreenRoute(
                                categoryId = categoryId
                            )
                        )
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
                    },
                    navigateToSummary = { categoryId, evaluationId ->
                        navController.navigate(
                            SummaryScreenRoute(categoryId = categoryId, evaluationId = evaluationId)
                        ) {
                            popUpTo(EvaluationScreenRoute(categoryId)) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<ConfigurationScreenRoute> {
                val viewModel = hiltViewModel<ConfigurationScreenViewModel>()
                ConfigurationScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToTerm = {
                        navController.navigate(
                            TermScreenRoute
                        )
                    },
                    navigateToCustomize = {
                        navController.navigate(
                            CustomizeScreenRoute
                        )
                    },
                    navigateToAbout = {

                    },
                    navigateToLogout = {
                        navController.navigate(LoginScreenRoute) {
                            popUpTo(ConfigurationScreenRoute) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<TermScreenRoute> {
                TermScreenRoot(
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<CustomizeScreenRoute> {
                val viewmodel = hiltViewModel<CustomizeScreenViewModel>()
                CustomizeScreenRoot(
                    viewModel = viewmodel,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<QuestionsScreenRoute> {
                val viewModel = hiltViewModel<QuestionsScreenViewModel>()
                QuestionsScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<PdfScreenRoute> {
                val viewModel = hiltViewModel<PdfScreenViewModel>()
                PdfScreenRoot(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<SummaryScreenRoute> {
                val viewModel = hiltViewModel<SummaryScreenViewModel>()
                SummaryScreenRoot(
                    viewModel = viewModel,
                    navigateToDetail = { categoryId ->
                        navController.navigateUp()
                    }
                )
            }

            composable<LoginScreenRoute> {
                val viewModel = hiltViewModel<LoginScreenViewModel>()
                LoginScreenRoot(
                    viewModel = viewModel,
                    navigateToHome = {
                        navController.navigate(HomeScreenRoute) {
                            popUpTo(LoginScreenRoute) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

        }
    }
}

