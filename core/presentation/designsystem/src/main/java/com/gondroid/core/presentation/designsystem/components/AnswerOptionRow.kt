package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.R
import com.gondroid.core.presentation.designsystem.extendedColors

enum class AnswerOptionState {
    Unselected,
    Selected,
    RevealedCorrect,
    RevealedIncorrect,
    CorrectAnswerHint
}

private data class AnswerOptionColors(
    val container: Color,
    val content: Color,
    val badgeContainer: Color,
    val badgeContent: Color
)

@Composable
private fun colorsFor(state: AnswerOptionState): AnswerOptionColors {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    return when (state) {
        AnswerOptionState.Unselected -> AnswerOptionColors(
            container = Color.Transparent,
            content = scheme.onSurface,
            badgeContainer = scheme.surfaceContainerHighest,
            badgeContent = scheme.onSurfaceVariant
        )

        AnswerOptionState.Selected -> AnswerOptionColors(
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            badgeContainer = scheme.secondaryContainer,
            badgeContent = scheme.onSecondaryContainer
        )

        AnswerOptionState.RevealedCorrect, AnswerOptionState.CorrectAnswerHint -> AnswerOptionColors(
            container = extended.successContainer,
            content = extended.onSuccessContainer,
            badgeContainer = extended.successContainer,
            badgeContent = extended.onSuccessContainer
        )

        AnswerOptionState.RevealedIncorrect -> AnswerOptionColors(
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            badgeContainer = scheme.errorContainer,
            badgeContent = scheme.onErrorContainer
        )
    }
}

@Composable
fun AnswerOptionRow(
    letter: String,
    text: String,
    state: AnswerOptionState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = colorsFor(state)

    Row(
        modifier = modifier
            .background(colors.container, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(colors.badgeContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall,
                color = colors.badgeContent,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = colors.content,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )

        when (state) {
            AnswerOptionState.RevealedCorrect, AnswerOptionState.CorrectAnswerHint -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.answer_correct),
                tint = colors.content,
                modifier = Modifier.size(18.dp)
            )

            AnswerOptionState.RevealedIncorrect -> Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.answer_incorrect),
                tint = colors.content,
                modifier = Modifier.size(18.dp)
            )

            else -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAnswerOptionRowStates() {
    MTCQuizTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            AnswerOptionRow(letter = "A", text = "Opción sin seleccionar", state = AnswerOptionState.Unselected, onClick = {})
            AnswerOptionRow(letter = "B", text = "Opción seleccionada (sin verificar)", state = AnswerOptionState.Selected, onClick = {})
            AnswerOptionRow(letter = "C", text = "Respuesta correcta revelada", state = AnswerOptionState.RevealedCorrect)
            AnswerOptionRow(letter = "D", text = "Respuesta incorrecta elegida", state = AnswerOptionState.RevealedIncorrect)
            AnswerOptionRow(letter = "A", text = "Esta era la correcta (el usuario eligió otra)", state = AnswerOptionState.CorrectAnswerHint)
        }
    }
}
