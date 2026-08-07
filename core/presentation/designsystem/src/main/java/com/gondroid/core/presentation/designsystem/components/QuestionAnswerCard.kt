package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageScope
import com.gondroid.core.presentation.designsystem.MTCQuizTheme

data class AnswerOption(
    val letter: String,
    val text: String,
    val state: AnswerOptionState,
)

@Composable
fun QuestionAnswerCard(
    title: String,
    options: List<AnswerOption>,
    modifier: Modifier = Modifier,
    questionImages: List<String> = emptyList(),
    onOptionClick: ((index: Int) -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (questionImages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        items(questionImages) { name ->
                            QuestionImage(name = name)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEachIndexed { index, option ->
                    AnswerOptionRow(
                        modifier = Modifier.fillMaxWidth(),
                        letter = option.letter,
                        text = option.text,
                        state = option.state,
                        onClick = onOptionClick?.let { callback -> { callback(index) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionImage(name: String) {
    SubcomposeAsyncImage(
        model = "file:///android_asset/images/$name.webp",
        contentDescription = name,
        modifier = Modifier.size(150.dp),
        contentScale = ContentScale.Fit,
        loading = { QuestionImageStatePlaceholder(icon = Icons.Outlined.Image) },
        error = { QuestionImageStatePlaceholder(icon = Icons.Outlined.BrokenImage) },
    )
}

/**
 * Loading/error placeholder drawn by [QuestionImage]. Rendered from a Coil content slot
 * ([SubcomposeAsyncImageScope]) rather than a boolean state flag, so there is no race between
 * Compose's composition-phase reads and Coil's draw-phase callbacks.
 */
@Composable
private fun SubcomposeAsyncImageScope.QuestionImageStatePlaceholder(icon: ImageVector) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewQuestionAnswerCardNoImage() {
    MTCQuizTheme {
        QuestionAnswerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            title = "3.- ¿Cuál señal indica velocidad máxima?",
            options = listOf(
                AnswerOption("A", "Círculo rojo con número", AnswerOptionState.RevealedCorrect),
                AnswerOption("B", "Triángulo amarillo", AnswerOptionState.Unselected),
                AnswerOption("C", "Rombo azul", AnswerOptionState.Unselected),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewQuestionAnswerCardWithImages() {
    MTCQuizTheme {
        QuestionAnswerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            title = "1.- ¿Cuál de las siguientes señales es preventiva?",
            questionImages = listOf("q14_a_b2a", "q14_b_b2a", "q14_c_b2a"),
            options = listOf(
                AnswerOption("A", "Camino sinuoso", AnswerOptionState.Selected),
                AnswerOption("B", "No camiones", AnswerOptionState.Unselected),
                AnswerOption("C", "Ciclistas en pendiente", AnswerOptionState.Unselected),
            ),
            onOptionClick = {},
        )
    }
}
