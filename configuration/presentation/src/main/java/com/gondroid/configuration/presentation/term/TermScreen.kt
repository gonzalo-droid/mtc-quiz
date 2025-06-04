package com.gondroid.configuration.presentation.term

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat


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

    val context = LocalContext.current


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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {

            AndroidView(
                modifier = Modifier,
                factory = { ctx ->
                    TextView(ctx).apply {
                        text = HtmlCompat.fromHtml(
                            loadHtmlFromAssets(context),
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        )
                         movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            )


        }
    }
}


fun loadHtmlFromAssets(context: Context): String {
    return context.assets.open("html/term.html").bufferedReader().use { it.readText() }
}


@Preview(showBackground = true)
@Composable
fun TermScreenRootPreview() {
    TermScreen(
        onNavigateUp = {}
    )
}