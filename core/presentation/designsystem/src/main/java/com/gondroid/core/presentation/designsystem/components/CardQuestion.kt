package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.R

@Composable
fun CardQuestion(
    modifier: Modifier,
    title: String,
    image: Painter
){
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier,
                text = title ,// "${question.id}.- ${question.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Image(
                painter = image, //painterResource(id = R.drawable.card_background),
                contentDescription = "card_background",
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            )
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewCardQuestion(){
    MTCQuizTheme {
        CardQuestion(
            modifier = Modifier.fillMaxWidth(),
            title = "1.  Respecto de los 100 de control o regulación del tránsito.",
            image = painterResource(id = R.drawable.card_background)
        )
    }
}