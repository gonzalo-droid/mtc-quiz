package com.gondroid.mtcquiz.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@Composable
fun LinearProgressComponent(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    countProgress: String = "",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .height(8.dp)
                .weight(1f),
        )
        Text(
            text = countProgress,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewQuestionsScreenRoot() {

    MTCQuizTheme {
        LinearProgressComponent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            countProgress = "1/12"
        )
    }
}
