package com.gondroid.configuration.presentation

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.configuration.presentation.R
import com.gondroid.core.presentation.designsystem.MTCQuizTheme


private fun requestInAppReview(activity: Activity, context: Context) {
    val manager = ReviewManagerFactory.create(activity)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                // Google doesn't tell us if user actually rated.
                // Flow is complete — no further action needed.
            }
        } else {
            OpenAppInPlayStore().invoke(context)
        }
    }
}

@Composable
fun ConfigurationScreenRoot(
    viewModel: ConfigurationScreenViewModel,
    navigateBack: () -> Unit,
    navigateToTerm: () -> Unit,
    navigateToCustomize: () -> Unit,
    navigateToTarifas: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToLogout: () -> Unit,
) {

    val context = LocalContext.current
    val event = viewModel.event
    val state by viewModel.state.collectAsState()

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
        state = state,
        onNavigateUp = navigateBack,
        onAction = { action ->
            when (action) {
                ConfigurationAction.GoToAbout -> navigateToAbout()
                ConfigurationAction.GoToRating -> {
                    val activity = context as? Activity
                    if (activity != null) {
                        requestInAppReview(activity, context)
                    } else {
                        OpenAppInPlayStore().invoke(context)
                    }
                }

                ConfigurationAction.GoToSCustomize -> navigateToCustomize()
                ConfigurationAction.GoToTerm -> navigateToTerm()
                ConfigurationAction.GoToTarifas -> navigateToTarifas()
                ConfigurationAction.Logout -> {
                    viewModel.logout()
                }
                is ConfigurationAction.ToggleDarkMode -> {
                    viewModel.onAction(action)
                }
            }

        }
    )
}

@Composable
fun ItemList(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    state: ConfigurationState,
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.dark_mode),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { onAction(ConfigurationAction.ToggleDarkMode(it)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemList(
                    icon = Icons.Default.Category,
                    title = stringResource(R.string.custom_values),
                    onClick = { onAction(ConfigurationAction.GoToSCustomize) },
                )

                ItemList(
                    icon = Icons.Default.AccountBalance,
                    title = stringResource(R.string.term_and_conditions),
                    onClick = { onAction(ConfigurationAction.GoToTerm) },
                )

                ItemList(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = "Trámites asociados",
                    onClick = { onAction(ConfigurationAction.GoToTarifas) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = { onAction(ConfigurationAction.GoToRating) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Calificar app",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                TextButton(
                    onClick = { onAction(ConfigurationAction.GoToAbout) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Nosotros",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            val context = LocalContext.current
            val versionName = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfigurationScreenRootPreview() {
    MTCQuizTheme {
        ConfigurationScreen(
            state = ConfigurationState(),
            onNavigateUp = {},
            onAction = {}
        )
    }
}
