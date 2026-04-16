package com.gondroid.configuration.presentation.premium

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gondroid.core.presentation.designsystem.MTCQuizTheme

private val premiumGold = Color(0xFFFFB300)
private val premiumAmber = Color(0xFFFF8F00)
private val premiumDark = Color(0xFF1A1A2E)
private val premiumDarkEnd = Color(0xFF16213E)

private val screenGradient = Brush.verticalGradient(
    colors = listOf(premiumDark, premiumDarkEnd),
)
private val buttonGradient = Brush.horizontalGradient(
    colors = listOf(premiumGold, premiumAmber),
)

private const val TERMS_URL = "https://gonzalo-lozg.me/term/quote-anime/"
private const val PRIVACY_URL = "https://gonzalo-lozg.me/term/quote-anime/"

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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenGradient),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    premiumGold.copy(alpha = 0.3f),
                                    Color.Transparent,
                                ),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = premiumGold,
                    )
                }

                Spacer(Modifier.height(20.dp))

                if (isPremium) {
                    Text(
                        text = "¡Eres Premium!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Disfrutas de la app sin publicidad.\nGracias por tu apoyo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                    )
                } else {
                    Text(
                        text = "MTCQuiz Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Estudia sin interrupciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(28.dp))

                    // Benefits
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        BenefitItem(
                            icon = Icons.Default.Block,
                            title = "Sin anuncios",
                            subtitle = "Elimina todos los banners e intersticiales",
                        )
                        BenefitItem(
                            icon = Icons.Default.Speed,
                            title = "Experiencia fluida",
                            subtitle = "Navega sin interrupciones entre pantallas",
                        )
                        BenefitItem(
                            icon = Icons.Default.Favorite,
                            title = "Apoya el desarrollo",
                            subtitle = "Ayuda a mantener la app actualizada",
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Plan selector
                    Text(
                        text = "Elige tu plan",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    PlanCard(
                        label = "Anual",
                        price = "S/ 19.99",
                        period = "/año",
                        badge = "Mejor valor",
                        selected = true,
                        onClick = { },
                    )

                    Spacer(Modifier.height(24.dp))

                    // Subscribe button
                    Surface(
                        onClick = onSubscribe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = buttonGradient,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "Suscribirme ahora",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Restaurar compras",
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Disclaimer
                    Text(
                        text = "La suscripción se renovará automáticamente al final del período " +
                            "a menos que la canceles al menos 24 horas antes. Puedes gestionar " +
                            "tu suscripción desde los ajustes de Google Play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                    )

                    Spacer(Modifier.height(12.dp))

                    // Legal links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Términos de uso",
                            style = MaterialTheme.typography.bodySmall,
                            color = premiumGold.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
                            },
                        )
                        Text(
                            text = "  •  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.3f),
                        )
                        Text(
                            text = "Política de privacidad",
                            style = MaterialTheme.typography.bodySmall,
                            color = premiumGold.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                            },
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    label: String,
    price: String,
    period: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) premiumGold else Color.White.copy(alpha = 0.15f)
    val bgColor = if (selected) premiumGold.copy(alpha = 0.1f) else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            ),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (selected) premiumGold else Color.White.copy(alpha = 0.3f), CircleShape)
                        .then(
                            if (selected) Modifier.background(premiumGold, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    if (badge != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = premiumGold,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(premiumGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = premiumGold,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
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
