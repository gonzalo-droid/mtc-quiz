package com.gondroid.mtcquiz.presentation.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@Composable
fun DetailScreenRoot(
    viewModel: DetailScreenViewModel,
    navigateTo: (String?) -> Unit,
) {

    DetailScreen()

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier =
                        Modifier.clickable {

                        },
                    )
                },
                actions = {
                    Box(
                        modifier =
                        Modifier
                            .padding(8.dp)
                            .clickable {
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                text = "A1",
                modifier = Modifier,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,

                )
            Text(
                modifier = Modifier,
                text = "CLASE A - CATEGORÍA I",
                fontSize = 20.sp,
            )

            Text(
                modifier = Modifier,
                text = "* Licencia de conducir para conductores no profesionales",
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                modifier = Modifier,
                text = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                fontSize = 15.sp,

                )


            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction()
        }

    }
}

@Composable
fun ButtonsAction() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón primario
        Button(
            onClick = { /* Acción del botón primario */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Ver video para acceso gratis")
        }
        // Botón secundario
        OutlinedButton(
            onClick = { /* Acción del botón secundario */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Descargar PDF")
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewDetailScreenRoot() {
    MTCQuizTheme {
        DetailScreen()
    }
}
