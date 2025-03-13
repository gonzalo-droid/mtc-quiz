package com.gondroid.mtcquiz.presentation.screens.pdf


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme
import java.io.File
import java.io.IOException

@Composable
fun PdfScreenRoot(
    viewModel: PdfScreenViewModel,
    navigateBack: () -> Boolean,
) {

    val state = viewModel.state

    val context = LocalContext.current
    val pdfUri = remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        pdfUri.value = copyPdfToDownloads(context, "document.pdf")
    }


    pdfUri.value?.let { openPdf(context, it) }


    PdfScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is PdfScreenAction.Back -> navigateBack()
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    state: PdfDataState,
    onAction: (PdfScreenAction) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pdf - Balotario",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                },
                actions = {

                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
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
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp),
        ) {


        }

    }
}

fun copyPdfToDownloads(context: Context, fileName: String): Uri? {
    val resolver = context.contentResolver
    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val outputFile = File(downloadsDir, fileName)

    return try {
        // Copia el archivo desde assets a la carpeta Descargas
        context.assets.open("pdf/$fileName").use { inputStream ->
            outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Obtener la Uri del archivo en Descargas
        Uri.fromFile(outputFile)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
    }
    context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
}


@Preview(
    showBackground = true,
)
@Composable
fun PreviewPdfScreenRoot() {
    MTCQuizTheme {
        PdfScreen(
            state = PdfDataState(),
            onAction = {}
        )
    }
}
