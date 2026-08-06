package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gondroid.core.presentation.designsystem.MTCQuizTheme

@Composable
fun CardQuestion(
    modifier: Modifier,
    title: String,
    questionImages: List<String> = emptyList(),
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier,
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (questionImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    items(questionImages) { name ->
                        AsyncImage(
                            model = "file:///android_asset/images/$name.webp",
                            contentDescription = name,
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp),
                                ),
                            contentScale = ContentScale.Fit,
                            placeholder = rememberVectorPainter(Icons.Outlined.Image),
                            error = rememberVectorPainter(Icons.Outlined.BrokenImage),
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewCardQuestion() {
    MTCQuizTheme {
        CardQuestion(
            modifier = Modifier.fillMaxWidth(),
            title = "1.  Respecto de los 100 de control o regulación del tránsito.",
        )
    }
}
