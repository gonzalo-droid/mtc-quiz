package com.gondroid.mtcquiz.presentation.screens.login

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.ui.theme.MTCQuizTheme

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun LoginScreenRoot(
    viewModel: LoginScreenViewModel,
    navigateToDetail: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as? Activity

    LoginScreen(
        navigateToDetail = {},
        onAction = { action ->
            when (action) {
                LoginScreenAction.GoogleSignOn -> {
                    viewModel.launchGoogleSignIn(context)
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navigateToDetail: () -> Unit,
    onAction: (LoginScreenAction) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = { },
                actions = { },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.practice_to_exam),
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "Inicia sesión para continuar",
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )


            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onAction(
                        LoginScreenAction.GoogleSignOn
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text("Iniciar sesión con Google")
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { },
            ) {
                Text("Más tarde")
            }
        }
    }

}


@Preview(showBackground = true)
@Composable
fun ConfigurationScreenRootPreview() {
    MTCQuizTheme {
        LoginScreen(
            navigateToDetail = {},
            onAction = { }
        )
    }
}