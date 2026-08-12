package com.gondroid.pdf.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.ui.ObserveAsEvents
import com.gondroid.presentation.screens.util.Permissions.RequestPermissionIfNeeded
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfScreenRoot(
    viewModel: PdfScreenViewModel,
    navigateBack: () -> Boolean,
    navigateToPremium: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showUpsellDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.adsManager.preloadPdfInterstitial(context)
    }

    val activity = LocalContext.current as? Activity

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            PdfEvent.ShowInterstitial -> {
                val act = activity
                if (act != null) {
                    viewModel.adsManager.showPdfInterstitial(act) {
                        showUpsellDialog = true
                        viewModel.onInterstitialClosed()
                    }
                } else {
                    viewModel.onInterstitialClosed()
                }
            }
            PdfEvent.StartDownload -> viewModel.onDownloadStarting()
        }
    }

    if (showUpsellDialog) {
        com.gondroid.core.presentation.designsystem.components.PremiumUpsellDialog(
            onGoToPremium = {
                showUpsellDialog = false
                navigateToPremium()
            },
            onDismiss = { showUpsellDialog = false }
        )
    }

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
                Timber.e(e, "Error al convertir PDF a Bitmaps")
                renderedPages = emptyList()
            }
        }
    }

    LaunchedEffect(state.category.pdf) {
        state.category.pdf?.let {
            pdfUri = copyAssetToCache(context, "pdf/$it")
            viewModel.loading(false)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    if (state.shouldDownload) {
        DownloadPdfIfPermitted(
            context = context,
            nameFile = state.category.pdf
        ) { downloadedUri ->
            viewModel.onDownloadFinished()
            scope.launch {
                if (downloadedUri != null) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.success_download_pdf),
                        actionLabel = context.getString(R.string.open_pdf_action),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        openDownloadedPdf(context, downloadedUri)
                    }
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.failure_download_pdf))
                }
            }
        }
    }

    PdfScreen(
        state = state,
        renderedPages = renderedPages,
        pdfUri = pdfUri,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                is PdfAction.Back -> navigateBack()
                PdfAction.Downloading -> {
                    viewModel.onDownloadClicked()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    state: PdfState,
    onAction: (PdfAction) -> Unit,
    pdfUri: Uri?,
    renderedPages: List<Bitmap>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                PdfAction.Downloading
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.download),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onBackground
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
                                PdfAction.Back
                            )
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (pdfUri == null) {
                CircularProgress()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(renderedPages) { page ->
                        PdfPage(page = page)
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
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
    }
}

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 3f

@Composable
fun PdfPage(
    page: Bitmap,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isZoomed = scale > MIN_ZOOM_SCALE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(page.width.toFloat() / page.height.toFloat())
            .clipToBounds()
    ) {
        AsyncImage(
            model = page,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    detectPinchZoom { pan, zoom ->
                        val newScale = (scale * zoom).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                        val maxOffsetX = (size.width * (newScale - 1)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1)) / 2f
                        offset = if (newScale > MIN_ZOOM_SCALE) {
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            Offset.Zero
                        }
                        scale = newScale
                    }
                }
                .then(
                    if (isZoomed) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val maxOffsetX = (size.width * (scale - 1)) / 2f
                                val maxOffsetY = (size.height * (scale - 1)) / 2f
                                offset = Offset(
                                    x = (offset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = MIN_ZOOM_SCALE
                        offset = Offset.Zero
                    })
                }
        )
    }
}

private suspend fun PointerInputScope.detectPinchZoom(
    onGesture: (pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                    onGesture(panChange, zoomChange)
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
fun DownloadPdfIfPermitted(
    context: Context,
    nameFile: String,
    onResult: (Uri?) -> Unit
) {
    RequestPermissionIfNeeded {
        val downloadedUri = savePdfToDownloads(context, "pdf/$nameFile", nameFile)
        onResult(downloadedUri)
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

fun savePdfToDownloads(context: Context, assetPath: String, fileName: String): Uri? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, assetPath, fileName)
        } else {
            saveViaLegacyFile(context, assetPath, fileName)
        }
    } catch (e: Exception) {
        Timber.e(e, "Error al guardar PDF en Descargas")
        null
    }
}

private fun saveViaMediaStore(context: Context, assetPath: String, fileName: String): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { output ->
        context.assets.open(assetPath).use { input -> input.copyTo(output) }
    }
    values.clear()
    values.put(MediaStore.Downloads.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}

private fun saveViaLegacyFile(context: Context, assetPath: String, fileName: String): Uri {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val outputFile = File(downloadsDir, fileName)
    context.assets.open(assetPath).use { input ->
        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
}

fun openDownloadedPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_pdf_viewer), Toast.LENGTH_LONG).show()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPdfScreenRoot() {
    MTCQuizTheme {
        PdfScreen(
            state = PdfState(),
            onAction = {},
            pdfUri = null,
            renderedPages = listOf()
        )
    }
}
