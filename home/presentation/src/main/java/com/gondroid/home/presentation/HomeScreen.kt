package com.gondroid.home.presentation


import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gondroid.core.domain.model.Category
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.ui.BannerAdSlot


@RequiresPermission(Manifest.permission.INTERNET)
@Composable
fun HomeScreenRoot(
    viewModel: HomeScreenViewModel,
    navigateToDetail: (String) -> Unit,
    navigateToConfiguration: () -> Unit,
    navigateToPremium: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        bannerAdId = viewModel.bannerAdId,
        onAction = { action ->
            when (action) {
                is HomeAction.OnClickCategory -> navigateToDetail(action.categoryId)
                is HomeAction.GoToConfiguration -> navigateToConfiguration()
                is HomeAction.GoToPremium -> navigateToPremium()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    bannerAdId: String,
    onAction: (HomeAction) -> Unit,
) {

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "home_screen"
            },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.test_evaluation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                },
                actions = {
                    if (!state.isPremium) {
                        IconButton(onClick = { onAction(HomeAction.GoToPremium) }) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "premium",
                                tint = Color(0xFFFFB300),
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                    onAction(HomeAction.GoToConfiguration)
                                },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "menu_button",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )

                    }
                },
            )
        },
        bottomBar = {
            BannerAdSlot(bannerAdId = bannerAdId, isPremium = state.isPremium)
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.practice_to_evaluation),
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.subtitle_message_home),
                style = MaterialTheme.typography.bodyLarge
            )

            if (state.streak > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Racha",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${state.streak} día${if (state.streak > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            state.categories.forEach { category ->
                CardCategoryItem(
                    item = category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onItemSelected = {
                        onAction(HomeAction.OnClickCategory(category.id))
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

        }
    }
}

@Composable
fun CardCategoryItem(
    item: Category,
    modifier: Modifier = Modifier,
    onItemSelected: () -> Unit = {}
) {

    val colors = categoryColors(
        category = item.category,
        fallback = CategoryColorScheme(
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
        ),
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.content,
        ),
        modifier = modifier.height(160.dp),
        onClick = onItemSelected
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.classType,
                    color = colors.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.category,
                    color = colors.content,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            AsyncImage(
                model = "file:///android_asset/anim/${item.examId}_card.png",
                contentDescription = "image_category",
                modifier = Modifier.align(Alignment.BottomEnd),
                contentScale = ContentScale.Fit
            )
        }
    }

}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewHomeScreenRoot() {
    MTCQuizTheme {
        HomeScreen(
            state = HomeState(
                categories = listOf(
                    Category(
                        id = "1",
                        title = "CLASE A - CATEGORIA 2",
                        category = "A-I",
                        classType = "CLASS A",
                        description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                        pdf = "CLASE_A_I.pdf"
                    ),
                    Category(
                        id = "2",
                        title = "CLASE A - CATEGORIA II-A",
                        category = "A-IIa",
                        classType = "CLASS A",
                        description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
                        pdf = "CLASE_A_I.pdf"
                    )
                )
            ),
            bannerAdId = "test-banner-id",
            onAction = {},
        )
    }
}
