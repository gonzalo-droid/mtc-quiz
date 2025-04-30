package com.gondroid.mtcquiz.presentation.screens.pdf


import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.presentation.screens.util.Permissions.RequestPermissionIfNeeded
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


@Composable
fun PdfScreenRoot(
    viewModel: PdfScreenViewModel,
    navigateBack: () -> Boolean,
) {
    val state = viewModel.state
    val context = LocalContext.current

    val pdfBitmapConverter = remember {
        PdfBitmapConverter(context)
    }
    var pdfUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var renderedPages by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri ->
            try {
                renderedPages = pdfBitmapConverter.pdfToBitmaps(uri)
            } catch (e: Exception) {
                Log.e("PDF_RENDER", "Error al convertir PDF a Bitmaps", e)
                renderedPages = emptyList()
            }
        }
    }

    var searchResults by remember {
        mutableStateOf(emptyList<SearchResults>())
    }

    LaunchedEffect(state.category.pdf) {
        state.category.pdf?.let {
            pdfUri = copyAssetToCache(context, "pdf/$it")
            viewModel.loading(false)
        }
    }

    var showMessage by remember { mutableStateOf<String?>(null) }

    if (state.shouldDownload) {
        DownloadPdfIfPermitted(context = context) { success ->
            showMessage =
                if (success) context.getString(R.string.success_download_pdf) else context.getString(
                    R.string.failure_download_pdf
                )
            viewModel.downloading(false)
        }
    }

    showMessage?.let {
        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        showMessage = null
    }

    PdfScreen(
        state = state,
        renderedPages = renderedPages,
        pdfUri = pdfUri,
        searchResults = searchResults,
        onValueChange = { newSearchText ->
            pdfBitmapConverter.renderer?.let { renderer ->
                scope.launch(Dispatchers.Default) {
                    searchResults = (0 until renderer.pageCount).map { index ->
                        renderer.openPage(index).use { page ->
                            val results = page.searchText(newSearchText)

                            val matchedRects = results.map {
                                it.bounds.first()
                            }

                            SearchResults(
                                page = index,
                                results = matchedRects
                            )
                        }
                    }
                }
            }
        },
        emptyListSearch = {
            searchResults = emptyList()
        },
        onAction = { action ->
            when (action) {
                is PdfScreenAction.Back -> navigateBack()
                PdfScreenAction.Downloading -> {
                    viewModel.downloading(true)
                }
            }
        },
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    state: PdfDataState,
    onAction: (PdfScreenAction) -> Unit,
    onValueChange: (String) -> Unit,
    emptyListSearch: () -> Unit,
    pdfUri: Uri?,
    renderedPages: List<Bitmap>,
    searchResults: List<SearchResults>,
) {

    var searchText by remember {
        mutableStateOf("")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier.clickable {
                            onAction(
                                PdfScreenAction.Downloading
                            )
                        },
                    ) {
                        Text(
                            text = "Descargar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ArrowBack",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier =
                            Modifier.clickable {
                                onAction(
                                    PdfScreenAction.Back,
                                )
                            },
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (pdfUri == null) {
                CircularProgress()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(renderedPages) { index, page ->
                            PdfPage(
                                page = page,
                                searchResults = searchResults.find { it.page == index }
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 35) {
                        OutlinedTextField(
                            value = searchText,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchText = ""
                                        emptyListSearch()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            onValueChange = { newSearchText ->
                                searchText = newSearchText
                                onValueChange(newSearchText)
                            }
                        )
                    }
                }
            }

            if (state.shouldDownload) {
                CircularProgress()
            }
        }
    }
}

@Composable
fun CircularProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun PdfPage(
    page: Bitmap,
    modifier: Modifier = Modifier,
    searchResults: SearchResults? = null
) {

    AsyncImage(
        model = page,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(page.width.toFloat() / page.height.toFloat())
            .drawWithContent {
                drawContent()

                val scaleFactorX = size.width / page.width
                val scaleFactorY = size.height / page.height

                searchResults?.results?.forEach { rect ->
                    val adjustedRect = RectF(
                        rect.left * scaleFactorX,
                        rect.top * scaleFactorY,
                        rect.right * scaleFactorX,
                        rect.bottom * scaleFactorY
                    )

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(
                            x = adjustedRect.left,
                            y = adjustedRect.top
                        ),
                        size = Size(
                            width = adjustedRect.width(),
                            height = adjustedRect.height()
                        ),
                        cornerRadius = CornerRadius(5.dp.toPx())
                    )
                }
            }
    )
}

@Composable
fun DownloadPdfIfPermitted(
    context: Context,
    onResult: (Boolean) -> Unit
) {
    RequestPermissionIfNeeded {
        val success = savePdfToDownloads(context, "pdf/CLASE_A_I.pdf", "CLASE_A_I.pdf")
        onResult(success)
    }
}


fun copyAssetToCache(context: Context, assetPath: String): Uri {
    val file = File(context.cacheDir, assetPath.substringAfterLast("/"))
    context.assets.open(assetPath).use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

fun savePdfToDownloads(context: Context, assetPath: String, fileName: String): Boolean {
    return try {
        val inputStream = context.assets.open(assetPath)
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(downloadsDir, fileName)

        FileOutputStream(outputFile).use { output ->
            inputStream.copyTo(output)
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewPdfScreenRoot() {
    MTCQuizTheme {
        PdfScreen(
            state = PdfDataState(),
            onAction = {},
            pdfUri = null,
            renderedPages = listOf(),
            onValueChange = {},
            emptyListSearch = {},
            searchResults = listOf(),
        )
    }
}