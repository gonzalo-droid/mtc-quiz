package com.gondroid.configuration.presentation

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    navigateToStats: () -> Unit = {},
    navigateToHistory: () -> Unit = {},
    navigateToPremium: () -> Unit = {},
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
                ConfigurationAction.GoToStats -> navigateToStats()
                ConfigurationAction.GoToHistory -> navigateToHistory()
                ConfigurationAction.GoToPremium -> navigateToPremium()
                ConfigurationAction.Logout -> {
                    viewModel.logout()
                }
                is ConfigurationAction.ToggleDarkMode -> {
                    viewModel.onAction(action)
                }
                is ConfigurationAction.SetThemeMode -> {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Mi progreso ---
            SectionTitle("Mi progreso")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemList(
                    icon = Icons.Default.BarChart,
                    title = "Estadísticas",
                    onClick = { onAction(ConfigurationAction.GoToStats) },
                )
                ItemList(
                    icon = Icons.Default.History,
                    title = "Historial de evaluaciones",
                    onClick = { onAction(ConfigurationAction.GoToHistory) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Configuración ---
            SectionTitle("Configuración")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val themeIcon = when (state.themeMode) {
                                "dark" -> Icons.Default.DarkMode
                                "light" -> Icons.Default.LightMode
                                else -> Icons.Default.LightMode
                            }
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Apariencia",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro").forEach { (mode, label) ->
                                val selected = state.themeMode == mode
                                Surface(
                                    onClick = { onAction(ConfigurationAction.SetThemeMode(mode)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                ItemList(
                    icon = Icons.Default.Category,
                    title = stringResource(R.string.custom_values),
                    onClick = { onAction(ConfigurationAction.GoToSCustomize) },
                )
                if (state.isPremium) {
                    ItemList(
                        icon = Icons.Default.WorkspacePremium,
                        title = "Premium ✓",
                        onClick = { onAction(ConfigurationAction.GoToPremium) },
                    )
                } else {
                    Surface(
                        onClick = { onAction(ConfigurationAction.GoToPremium) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFFB300),
                                            Color(0xFFFF8F00),
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hazte Premium",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Estudia sin anuncios",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Información ---
            SectionTitle("Información")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                ItemList(
                    icon = Icons.Default.Star,
                    title = "Calificar app",
                    onClick = { onAction(ConfigurationAction.GoToRating) },
                )
                ItemList(
                    icon = Icons.Default.Info,
                    title = "Nosotros",
                    onClick = { onAction(ConfigurationAction.GoToAbout) },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
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
