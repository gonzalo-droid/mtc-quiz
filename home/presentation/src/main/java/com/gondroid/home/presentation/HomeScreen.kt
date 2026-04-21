package com.gondroid.home.presentation


import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.gondroid.core.data.local.CardType
import com.gondroid.core.domain.model.Category
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlin.math.absoluteValue


@RequiresPermission(Manifest.permission.INTERNET)
@Composable
fun HomeScreenRoot(
    viewModel: HomeScreenViewModel,
    navigateToDetail: (String) -> Unit,
    navigateToConfiguration: () -> Unit,
    navigateToPremium: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    val bannerAdId = viewModel.bannerAdId
    val adView = remember {
        AdView(context).apply {
            adUnitId = bannerAdId
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360)
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("adMobTest", "Banner ad was loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("adMobTest", "Banner ad failed to load: ${error.message}")
                }

                override fun onAdImpression() {
                    Log.d("adMobTest", "Banner ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("adMobTest", "Banner ad was clicked.")
                }
            }
        }
    }

    // Prevent loading the AdView if the app is in preview mode.
    val isInspectionMode = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspectionMode) {
            // [START load_ad]
            // Create an AdRequest and load the ad.
            adView.loadAd(AdRequest.Builder().build())
            // [END load_ad]
        }
    }

    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeAction.OnClickCategory -> navigateToDetail(action.categoryId)
                is HomeAction.GoToConfiguration -> navigateToConfiguration()
                is HomeAction.GoToPremium -> navigateToPremium()
            }
        },
        content = {
            if (!state.isPremium) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BannerAd(adView, Modifier)
                    }
                }
            }
        }
    )

    DisposableEffect(Unit) {
        // Destroy the AdView to prevent memory leaks when the screen is disposed.
        onDispose { adView.destroy() }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    content: @Composable () -> Unit
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
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp),
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

            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = stringResource(R.string.selected_category),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val pagerState = rememberPagerState(pageCount = { state.categories.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 40.dp)
            ) { index ->

                CardCategoryItem(
                    pagerState = pagerState,
                    index = index,
                    item = state.categories[index],
                    onItemSelected = {
                        onAction(HomeAction.OnClickCategory(state.categories[index].id))
                    })
            }

            Spacer(modifier = Modifier.weight(1f))

            content()

        }
    }
}

@Composable
fun CardCategoryItem(
    pagerState: PagerState,
    index: Int,
    item: Category,
    onItemSelected: () -> Unit = {}
) {

    val pageOffSet = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .height(400.dp)
            .graphicsLayer {
                lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - pageOffSet.absoluteValue.coerceIn(0f, 1f)
                ).also { scale ->
                    scaleX = scale
                    scaleY = scale
                }
                alpha = lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - pageOffSet.absoluteValue.coerceIn(0f, 1f)
                )
            },
        onClick = onItemSelected
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.classType,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.category,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 4,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Image(
                painter = painterResource(id = item.image?:R.drawable.a1_card),
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
                        image = CardType.A2A.drawable,
                        pdf = "CLASE_A_I.pdf"
                    ),
                    Category(
                        id = "2",
                        title = "CLASE A - CATEGORIA II-A",
                        category = "A-IIa",
                        classType = "CLASS A",
                        description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
                        image = CardType.A1.drawable,
                        pdf = "CLASE_A_I.pdf"
                    )
                )
            ),
            onAction = {},
            content = {}
        )
    }
}
