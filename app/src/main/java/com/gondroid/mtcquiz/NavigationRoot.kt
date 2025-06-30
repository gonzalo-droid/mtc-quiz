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
import com.gondroid.configuration.presentation.ConfigurationScreenRoot
import com.gondroid.configuration.presentation.ConfigurationScreenViewModel
import com.gondroid.configuration.presentation.customize.CustomizeScreenRoot
import com.gondroid.configuration.presentation.customize.CustomizeScreenViewModel
import com.gondroid.configuration.presentation.term.TermScreenRoot
import com.gondroid.core.presentation.ui.ConfigurationScreenRoute
import com.gondroid.core.presentation.ui.CustomizeScreenRoute
import com.gondroid.core.presentation.ui.DetailScreenRoute
import com.gondroid.core.presentation.ui.EvaluationScreenRoute
import com.gondroid.core.presentation.ui.HomeScreenRoute
import com.gondroid.core.presentation.ui.LoginScreenRoute
import com.gondroid.core.presentation.ui.PdfScreenRoute
import com.gondroid.core.presentation.ui.QuestionsScreenRoute
import com.gondroid.core.presentation.ui.SummaryScreenRoute
import com.gondroid.core.presentation.ui.TermScreenRoute
import com.gondroid.detail.presentation.DetailScreenRoot
import com.gondroid.detail.presentation.DetailScreenViewModel
import com.gondroid.evaluation.presentation.EvaluationScreenRoot
import com.gondroid.evaluation.presentation.EvaluationScreenViewModel
import com.gondroid.evaluation.presentation.summary.SummaryScreenRoot
import com.gondroid.evaluation.presentation.summary.SummaryScreenViewModel
import com.gondroid.home.presentation.HomeScreenRoot
import com.gondroid.home.presentation.HomeScreenViewModel
import com.gondroid.pdf.presentation.PdfScreenRoot
import com.gondroid.pdf.presentation.PdfScreenViewModel
import com.gondroid.questionreview.presentation.QuestionsScreenRoot
import com.gondroid.questionreview.presentation.QuestionsScreenViewModel

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
                        // TODO: Implementar navegación a About
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

