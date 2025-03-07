package com.gondroid.mtcquizz.presentation.screens.home


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.gondroid.mtcquizz.ui.theme.MTCQuizzTheme
import kotlin.math.absoluteValue

@Composable
fun HomeScreenRoot(
    viewModel: HomeScreenViewModel,
    navigateTo: (String?) -> Unit,
) {

    HomeScreen()

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MTC",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
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
            Text("Examen de conocimientos para postulantes a licencias de conducir")

            val pagerState = rememberPagerState(pageCount = { 10 })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(50.dp)
            ) { index ->
                CardContent(pagerState, index)
            }

        }

    }
}

@Composable
fun CardContent(pagerState: PagerState, index: Int) {

    val pageOffSet = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Blue),
        modifier = Modifier
            .height(600.dp)
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
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Card $index", color = Color.White)
        }
    }

}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewHomeScreenRoot() {
    MTCQuizzTheme {
        HomeScreen()
    }
}
