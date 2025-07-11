package com.gondroid.configuration.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.presentation.util.OpenAppInPlayStore
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import com.gondroid.presentation.util.OpenAppInPlayStore


@Composable
fun ConfigurationScreenRoot(
    viewModel: ConfigurationScreenViewModel,
    navigateBack: () -> Unit,
    navigateToTerm: () -> Unit,
    navigateToCustomize: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToLogout: () -> Unit,
) {

    val context = LocalContext.current
    val event = viewModel.event

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                ConfigurationEvent.Success -> {
                    navigateToLogout()
                }
            }
        }

    }

    ConfigurationScreen(
        onNavigateUp = navigateBack,
        onAction = { action ->
            when (action) {
                ConfigurationAction.GoToAbout -> navigateToAbout()
                ConfigurationAction.GoToRating -> {
                    OpenAppInPlayStore().invoke(context)
                }

                ConfigurationAction.GoToSCustomize -> navigateToCustomize()
                ConfigurationAction.GoToTerm -> navigateToTerm()
                ConfigurationAction.Logout -> {
                    viewModel.logout()
                }
            }

        }
    )
}

@Composable
fun ItemList(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    onItemClick: () -> Unit,
) {

    Column(modifier = modifier.clickable {
        onItemClick()
    }) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onItemClick, modifier = Modifier) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateUp: () -> Unit,
    onAction: (ConfigurationAction) -> Unit
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
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(50.dp))

            ItemList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                icon = Icons.Default.Category,
                title = stringResource(R.string.custom_values),
                onItemClick = {
                    onAction(
                        ConfigurationAction.GoToSCustomize
                    )
                }
            )

            ItemList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                icon = Icons.Default.AccountBalance,
                title = stringResource(R.string.term_and_conditions),
                onItemClick = {
                    onAction(
                        ConfigurationAction.GoToTerm
                    )
                }
            )

            ItemList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                icon = Icons.Default.Star,
                title = stringResource(R.string.ranting_app),
                onItemClick = {
                    onAction(
                        ConfigurationAction.GoToRating
                    )
                }
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
                    text = stringResource(R.string.about_us),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        onAction(
                            ConfigurationAction.Logout
                        )
                    }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = stringResource(R.string.logout),
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
    MTCQuizTheme {
        ConfigurationScreen(
            onNavigateUp = {},
            onAction = {}
        )
    }
}