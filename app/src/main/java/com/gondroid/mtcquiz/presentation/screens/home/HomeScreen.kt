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
import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import kotlin.math.absoluteValue

@Composable
fun HomeScreenRoot(
    viewModel: HomeScreenViewModel,
    navigateTo: (String) -> Unit,
) {

    val state = viewModel.state

    HomeScreen(
        state = state,
        onAction = { action ->
            when(action) {
                is HomeScreenAction.OnClickCategory -> navigateTo(action.categoryId)
            }

        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeDataState,
    onAction: (HomeScreenAction) -> Unit
) {
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

            val items = listOf(
                Category(
                    id = "1",
                    title = "CLASE A - CATEGORIA I",
                    category = "A1",
                    type = "CLASE A",
                    image = "a",
                    description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A."
                ),
                Category(
                    id = "1",
                    title = "CLASE A - CATEGORIA I",
                    category = "A2",
                    type = "CLASE A",
                    image = "a",
                    description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A."
                ),
                Category(
                    id = "1",
                    title = "CLASE A - CATEGORIA I",
                    category = "A3",
                    type = "CLASE A",
                    image = "a",
                    description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A."
                ),
            )

            val pagerState = rememberPagerState(pageCount = { items.size })
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 40.dp)
            ) { index ->

                CardCategoryItem(
                    pagerState = pagerState,
                    index = index,
                    item = items[index],
                    onItemSelected = {
                        onAction(HomeScreenAction.OnClickCategory(items[index].id))
                    })
            }

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
        onClick = onItemSelected
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.type,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.category,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
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
        HomeScreen(
            state = HomeDataState(),
            onAction = {}
        )
    }
}
