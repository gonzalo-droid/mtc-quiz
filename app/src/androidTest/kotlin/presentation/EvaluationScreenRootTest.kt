package presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Question
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationDataState
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreen
import com.gondroid.mtcquiz.presentation.screens.evaluation.EvaluationScreenRoot
import org.junit.Rule
import org.junit.Test
import util.MainDispatcherRule

class EvaluationScreenRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()


    @Test
    fun evaluationScreen_whenInitScreen_showFirstQuestion() {
        val state = previewEvaluationState()

        composeRule.setContent {
            MaterialTheme {
                EvaluationScreen(
                    state = state,
                    onAction = {},
                    showCancelDialog = false
                )
            }
        }

        composeRule.onNodeWithText("1.- Pregunta 1").assertExists()
    }
}


fun previewEvaluationState() = EvaluationDataState(
    questions = (1..5).map {
        Question(
            id = it,
            title = "Pregunta $it",
            options = listOf("Opción 1", "Opción 2", "Opción 3", "Opción 4"),
            answer = "a"
        )
    }.toMutableList(),
    question = Question(
        id = 1,
        title = "Pregunta 1",
        options = listOf("Opción 1", "Opción 2", "Opción 3", "Opción 4"),
        answer = "a"
    ),
    indexQuestion = 0,
    answerWasSelected = false,
    answerWasVerified = false,
    isFinishExam = false,
    category = Category(
        id = "1",
        title = "Categoría 1",
        description = "Descripción de la categoría 1",
    ),
    totalMinutes = 1
)