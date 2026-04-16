package com.gondroid.configuration.presentation.premium

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gondroid.core.presentation.designsystem.MTCQuizTheme

@Composable
fun PremiumScreenRoot(
    viewModel: PremiumViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
) {
    val isPremium by viewModel.isPremium.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activity = LocalContext.current as? Activity

    PremiumScreen(
        isPremium = isPremium,
        isLoading = isLoading,
        onSubscribe = { activity?.let { viewModel.subscribe(it) } },
        onRestore = { viewModel.restorePurchases() },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    isPremium: Boolean,
    isLoading: Boolean,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    navigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFFB300),
            )

            Spacer(Modifier.height(24.dp))

            if (isPremium) {
                Text(
                    text = "¡Eres Premium!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Disfrutas de la app sin publicidad",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "MTCQuiz Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Estudia sin interrupciones",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(32.dp))

                BenefitItem(
                    icon = Icons.Default.Block,
                    text = "Sin anuncios publicitarios",
                )
                Spacer(Modifier.height(12.dp))
                BenefitItem(
                    icon = Icons.Default.Speed,
                    text = "Experiencia fluida y sin interrupciones",
                )
                Spacer(Modifier.height(12.dp))
                BenefitItem(
                    icon = Icons.Default.Favorite,
                    text = "Apoyás el desarrollo de la app",
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Suscribirme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Restaurar compras")
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFB300),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumScreenPreview() {
    MTCQuizTheme {
        PremiumScreen(
            isPremium = false,
            isLoading = false,
            onSubscribe = {},
            onRestore = {},
            navigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumScreenAlreadyPremiumPreview() {
    MTCQuizTheme {
        PremiumScreen(
            isPremium = true,
            isLoading = false,
            onSubscribe = {},
            onRestore = {},
            navigateBack = {},
        )
    }
}
