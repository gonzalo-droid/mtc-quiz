package com.gondroid.configuration.presentation.term

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.core.presentation.designsystem.components.WebViewWithOffline


@Composable
fun TermScreenRoot(
    navigateBack: () -> Unit
) {
    TermScreen(
        onNavigateUp = navigateBack
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermScreen(
    onNavigateUp: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        WebViewWithOffline(
            url = "https://gonzalo-lozg.me/term/quote-anime/",
            modifier = Modifier.padding(paddingValues),
        )
    }
}


@Preview(showBackground = true)
@Composable
fun TermScreenRootPreview() {
    TermScreen(
        onNavigateUp = {}
    )
}
