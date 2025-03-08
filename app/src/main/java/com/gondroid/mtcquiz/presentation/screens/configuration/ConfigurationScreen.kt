package com.gondroid.mtcquiz.presentation.screens.configuration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun ConfigurationScreenRoot(
    viewModel : ConfigurationScreenViewModel,
    navigateBack: () -> Unit
) {

    ConfigurationScreen(
        onNavigateUp = navigateBack
    )
}

@Composable
fun ItemList(
    icon: ImageVector,
    title: String,
    onItemClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
        }
        IconButton(onClick = onItemClick, modifier = Modifier) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateUp: () -> Unit
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
                text = "Hola,",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Eren",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(50.dp))


            ItemList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                icon = Icons.Default.Favorite,
                title = "Términos & Condiciones",
                onItemClick = {  }
            )

            ItemList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                icon = Icons.Default.Star,
                title = "Califica la aplicación",
                onItemClick = {  }
            )


            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "Nosotros",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}


@Preview(showBackground = true)
@Composable
fun ConfigurationScreenRootPreview() {
    ConfigurationScreen(
        onNavigateUp = {}
    )
}