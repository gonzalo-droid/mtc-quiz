package com.gondroid.configuration.presentation.customize

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.configuration.presentation.R
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.extendedColors
import kotlin.math.roundToInt

private const val STEP = 5

// Los topes de los deslizadores son los del examen real con holgura, no los 1000 que
// admitía el campo de texto: ningún balotario llega a esa cifra y nadie estudia 16 horas
// seguidas. Un valor guardado fuera de rango no se recorta, amplía el deslizador.
private val MINUTES_RANGE = 5..120
private val QUESTIONS_RANGE = 5..100
private val PASS_MARK_RANGE = 50..100

@Composable
fun CustomizeScreenRoot(
    viewModel: CustomizeScreenViewModel,
    navigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val event = viewModel.event
    val context = LocalContext.current

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                is CustomizeEvent.Success -> {
                    Toast.makeText(context, "Ajustes guardados", Toast.LENGTH_LONG).show()
                }

                is CustomizeEvent.Error -> {
                    Toast.makeText(context, "No se pudieron guardar los ajustes", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    CustomizeScreen(
        state = state,
        onNavigateUp = navigateBack,
        onAction = { action ->
            when (action) {
                is CustomizeAction.UpdateValues -> viewModel.updateValues(
                    numberQuestions = action.numberQuestions,
                    timeToFinishEvaluation = action.timeToFinishEvaluation,
                    percentageToApprovedEvaluation = action.percentageToApprovedEvaluation
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(
    onNavigateUp: () -> Unit,
    onAction: (CustomizeAction) -> Unit,
    state: CustomizeState
) {
    var minutes by rememberSaveable { mutableIntStateOf(0) }
    var questions by rememberSaveable { mutableIntStateOf(0) }
    var passMark by rememberSaveable { mutableIntStateOf(0) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    // Los valores guardados llegan después del primer frame; se copian una sola vez para no
    // pisar lo que el usuario esté moviendo.
    LaunchedEffect(state) {
        if (!loaded && state.numberQuestions.isNotBlank()) {
            minutes = state.timeToFinishEvaluation.toIntOrNull() ?: 40
            questions = state.numberQuestions.toIntOrNull() ?: 40
            passMark = state.percentageToApprovedEvaluation.toIntOrNull() ?: 80
            loaded = true
        }
    }

    val setup = EvaluationSetup(minutes = minutes, questions = questions, passPercentage = passMark)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.custimize_setting),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.customize_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Dial(
                        label = stringResource(R.string.time_to_evaluation),
                        readout = stringResource(R.string.customize_minutes, minutes),
                        value = minutes,
                        range = MINUTES_RANGE,
                        onValueChange = { minutes = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Dial(
                        label = stringResource(R.string.number_of_question_to_evaluation),
                        readout = questions.toString(),
                        value = questions,
                        range = QUESTIONS_RANGE,
                        onValueChange = { questions = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Dial(
                        label = stringResource(R.string.percentage_approbe_to_evaluation),
                        readout = stringResource(R.string.customize_percentage, passMark),
                        value = passMark,
                        range = PASS_MARK_RANGE,
                        onValueChange = { passMark = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PaceChip(setup = setup)
            Spacer(modifier = Modifier.height(8.dp))
            Summary(setup = setup)

            Spacer(modifier = Modifier.height(24.dp))
            ButtonsAction(
                enabled = loaded &&
                    minutes in 1..1000 && questions in 1..1000 && passMark in 1..100,
                modifier = Modifier.fillMaxWidth(),
                updateData = {
                    onAction(
                        CustomizeAction.UpdateValues(
                            numberQuestions = questions.toString(),
                            timeToFinishEvaluation = minutes.toString(),
                            percentageToApprovedEvaluation = passMark.toString()
                        )
                    )
                }
            )
        }
    }
}

/** Una fila: etiqueta, cifra grande y deslizador. */
@Composable
private fun Dial(
    label: String,
    readout: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    // Un valor heredado fuera de rango extiende el deslizador en vez de recortarse solo.
    val from = minOf(range.first, value).toFloat()
    val to = maxOf(range.last, value).toFloat()

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = readout,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            modifier = Modifier.semantics { contentDescription = label },
            value = value.toFloat().coerceIn(from, to),
            valueRange = from..to,
            onValueChange = { raw ->
                onValueChange((raw / STEP).roundToInt() * STEP)
            }
        )
    }
}

/** Cuánto tiempo queda por pregunta, y si eso es cómodo o no. */
@Composable
private fun PaceChip(setup: EvaluationSetup) {
    val (container, content) = when (setup.pace) {
        Pace.COMFORTABLE ->
            MaterialTheme.extendedColors.successContainer to
                MaterialTheme.extendedColors.onSuccessContainer
        Pace.TIGHT ->
            MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        Pace.AGAINST_THE_CLOCK ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }
    val name = when (setup.pace) {
        Pace.COMFORTABLE -> stringResource(R.string.customize_pace_comfortable)
        Pace.TIGHT -> stringResource(R.string.customize_pace_tight)
        Pace.AGAINST_THE_CLOCK -> stringResource(R.string.customize_pace_against_clock)
    }
    val perQuestion = if (setup.secondsPerQuestion >= 60) {
        stringResource(
            R.string.customize_pace_minutes,
            setup.secondsPerQuestion / 60,
            setup.secondsPerQuestion % 60
        )
    } else {
        stringResource(R.string.customize_pace_seconds, setup.secondsPerQuestion)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(content, CircleShape))
        Text(
            text = "$name · $perQuestion",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = content
        )
    }
}

/** El número que el usuario venía calculando de cabeza: cuántas hay que acertar. */
@Composable
private fun Summary(setup: EvaluationSetup) {
    val text = if (setup.allowedMistakes == 0) {
        stringResource(R.string.customize_summary_strict, setup.correctToPass)
    } else {
        stringResource(
            R.string.customize_summary,
            setup.correctToPass,
            setup.questions,
            setup.allowedMistakes
        )
    }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Start
    )
}

@Composable
fun ButtonsAction(
    modifier: Modifier = Modifier,
    updateData: () -> Unit = {},
    enabled: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            enabled = enabled,
            onClick = updateData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.update_preferences_evaluation))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomizeScreenRootPreview() {
    MTCQuizTheme {
        CustomizeScreen(
            onNavigateUp = {},
            onAction = {},
            state = CustomizeState(
                numberQuestions = "40",
                timeToFinishEvaluation = "40",
                percentageToApprovedEvaluation = "80"
            )
        )
    }
}

@Preview(showBackground = true, name = "Contrarreloj")
@Composable
fun CustomizeScreenAgainstClockPreview() {
    MTCQuizTheme {
        CustomizeScreen(
            onNavigateUp = {},
            onAction = {},
            state = CustomizeState(
                numberQuestions = "100",
                timeToFinishEvaluation = "20",
                percentageToApprovedEvaluation = "90"
            )
        )
    }
}
