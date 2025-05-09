package com.gondroid.mtcquiz.presentation.screens.Customize.customize

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.gondroid.mtcquiz.presentation.screens.configuration.customize.CustomizeScreenAction
import com.gondroid.mtcquiz.presentation.screens.configuration.customize.CustomizeScreenViewModel
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@Composable
fun CustomizeScreenRoot(
    viewModel: CustomizeScreenViewModel,
    navigateBack: () -> Unit,
) {

    val content = LocalContext.current

    CustomizeScreen(
        onNavigateUp = navigateBack,
        onAction = { action ->
            when (action) {
                CustomizeScreenAction.ResetValues -> TODO()
                CustomizeScreenAction.UpdateValues -> TODO()
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(
    onNavigateUp: () -> Unit,
    onAction: (CustomizeScreenAction) -> Unit
) {
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
                label = "Límita el número de preguntas para tu evalución",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ItemField(
                label = "Porcentage de preguntas correctas para aprobar",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))
            ButtonsAction(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                updateData = {
                    onAction(
                        CustomizeScreenAction.UpdateValues
                    )
                },
                resetData = {
                    onAction(
                        CustomizeScreenAction.ResetValues
                    )
                }
            )
        }
    }
}

@Composable
fun ItemField(
    label: String,
    modifier: Modifier
) {

    var value by remember { mutableStateOf("") }

    Text(
        style = MaterialTheme.typography.bodyLarge,
        text = label
    )
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { newValue ->
            val valueInt = newValue.toIntOrNull()
            if (valueInt != null && valueInt in 1..100) {
                value = newValue
            } else if (newValue.isEmpty()) {
                value = ""
            }
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
    )

}


@Composable
fun ButtonsAction(
    modifier: Modifier = Modifier,
    updateData: () -> Unit = {},
    resetData: () -> Unit = {},
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = updateData,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(text = "Actualizar valores")
        }

        OutlinedButton(
            onClick = resetData,
            modifier = Modifier
                .fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Restablecer valores")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CustomizeScreenRootPreview() {
    MTCQuizTheme {
        CustomizeScreen(
            onNavigateUp = {},
            onAction = {}
        )
    }
}