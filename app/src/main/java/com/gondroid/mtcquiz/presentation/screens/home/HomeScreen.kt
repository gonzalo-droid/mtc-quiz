package com.gondroid.mtcquiz.presentation.screens.home


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
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
                        text = "MTC - Cuestionario",
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
                .padding(vertical = 16.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "Examen de conocimientos para postulantes a licencias de conducir"
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                text = "Selecciona tu categoria",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val pagerState = rememberPagerState(pageCount = { 10 })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 40.dp)
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
        colors = CardDefaults.cardColors(containerColor = Color.Gray),
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
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "CLASE A",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A1",
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 3,
                    fontSize = 12.sp,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Image(
                painter = painterResource(id = R.drawable.card_background),
                contentDescription = "card_background",
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
        HomeScreen()
    }
}
