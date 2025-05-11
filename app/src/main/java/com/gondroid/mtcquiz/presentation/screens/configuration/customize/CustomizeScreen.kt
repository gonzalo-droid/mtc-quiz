package com.gondroid.mtcquiz.presentation.screens.configuration.customize

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@Composable
fun CustomizeScreenRoot(
    viewModel: CustomizeScreenViewModel,
    navigateBack: () -> Unit,
) {

    val state by viewModel.state.collectAsState()
    val event = viewModel.event
    val context = LocalContext.current

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                is CustomizeEvent.Success -> {
                    Toast.makeText(context, "Datos actualizados",Toast.LENGTH_LONG).show()
                }
                is CustomizeEvent.Error -> {
                    Toast.makeText(context, "Error al actualizar los datos",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    CustomizeScreen(
        state = state,
        onNavigateUp = navigateBack,
        onAction = { action ->
            when (action) {
                is CustomizeScreenAction.UpdateValues -> viewModel.updateValues(
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
    onAction: (CustomizeScreenAction) -> Unit,
    state: CustomizeDataState
) {

    var numberQuestions by remember { mutableStateOf("") }
    var timeToFinishEvaluation by remember { mutableStateOf("") }
    var percentageToApprovedEvaluation by remember { mutableStateOf("") }

    numberQuestions = state.numberQuestions
    timeToFinishEvaluation = state.timeToFinishEvaluation
    percentageToApprovedEvaluation = state.percentageToApprovedEvaluation

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.custimize_setting),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(40.dp))

            ItemField(
                value = timeToFinishEvaluation,
                label = stringResource(R.string.time_to_evaluation),
                subLabel = "1 - 1000",
                modifier = Modifier.fillMaxWidth()
            ) { newValue ->
                val valueInt = newValue.toIntOrNull()
                if (valueInt != null && valueInt in 1..1000) {
                    timeToFinishEvaluation = newValue
                } else if (newValue.isEmpty()) {
                    timeToFinishEvaluation = ""
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ItemField(
                value = numberQuestions,
                label = stringResource(R.string.number_of_question_to_evaluation),
                subLabel = "1 - 1000",
                modifier = Modifier.fillMaxWidth(),
            ) { newValue ->
                val valueInt = newValue.toIntOrNull()
                if (valueInt != null && valueInt in 1..1000) {
                    numberQuestions = newValue
                } else if (newValue.isEmpty()) {
                    numberQuestions = ""
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ItemField(
                value = percentageToApprovedEvaluation,
                label = stringResource(R.string.percentage_approbe_to_evaluation),
                subLabel = "1 - 100 (%)",
                modifier = Modifier.fillMaxWidth(),
            ) { newValue ->
                val valueInt = newValue.toIntOrNull()
                if (valueInt != null && valueInt in 1..100) {
                    percentageToApprovedEvaluation = newValue
                } else if (newValue.isEmpty()) {
                    percentageToApprovedEvaluation = ""

                }
            }

            Spacer(modifier = Modifier.weight(1f))
            ButtonsAction(
                enabled = numberQuestions.isNotBlank() && timeToFinishEvaluation.isNotBlank() && percentageToApprovedEvaluation.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                updateData = {
                    onAction(
                        CustomizeScreenAction.UpdateValues(
                            numberQuestions = numberQuestions,
                            timeToFinishEvaluation = timeToFinishEvaluation,
                            percentageToApprovedEvaluation = percentageToApprovedEvaluation
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun ItemField(
    label: String,
    subLabel: String = "",
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit
) {

    Text(
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        text = label
    )
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
    )
    if(value.isBlank()){
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            text = "Debe ingresar un valor"
        )
    } else {
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            text = subLabel
        )
    }


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
            modifier = Modifier
                .fillMaxWidth()

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
            state = CustomizeDataState()
        )
    }
}