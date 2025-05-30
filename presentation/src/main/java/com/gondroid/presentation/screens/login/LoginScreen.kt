package com.gondroid.presentation.screens.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.presentation.R
import com.gondroid.presentation.ui.theme.MTCQuizTheme

@Composable
fun LoginScreenRoot(
    viewModel: LoginScreenViewModel,
    navigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val event = viewModel.event

    LaunchedEffect(true) {
        event.collect { event ->
            when (event) {
                LoginEvent.Fail -> {
                    Toast.makeText(context, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }

                LoginEvent.Success -> {
                    Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }
        }
    }

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
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 32.dp)

            ) {

                Text(
                    modifier = Modifier,
                    text = stringResource(R.string.small_title_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    modifier = Modifier,
                    text = stringResource(R.string.medium_title_header),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(200.dp))

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

                    Text(stringResource(R.string.singing_google))
                }

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