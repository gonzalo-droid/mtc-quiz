package presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.gondroid.mtcquiz.ConfigurationScreenRoute
import com.gondroid.mtcquiz.DetailScreenRoute
import com.gondroid.mtcquiz.EvaluationScreenRoute
import com.gondroid.mtcquiz.HomeScreenRoute
import com.gondroid.mtcquiz.PdfScreenRoute
import com.gondroid.mtcquiz.QuestionsScreenRoute
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenRoot
import com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenRoot
import com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModel
import org.junit.Rule
import org.junit.Test

class NavigationRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController


    @Test
    fun navigationRoot_whenInitScreen_showLoginScreen(){
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MaterialTheme {
                NavHost(
                    navController = navController,
                    startDestination = HomeScreenRoute
                ) {
                    composable<HomeScreenRoute> {
                        val viewModel = hiltViewModel<HomeScreenViewModel>()

                        HomeScreenRoot(
                            viewModel = viewModel,
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
                }
            }
        }

        // Act - Navigate to detail screen
        composeRule.onNodeWithContentDescription("CLASE A").performClick()
    }
}