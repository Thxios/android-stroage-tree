package com.thxios.storagetree.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.thxios.storagetree.ui.navigation.AppDestination
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val navTarget by viewModel.navTarget.collectAsStateWithLifecycle()

    LaunchedEffect(navTarget) {
        when (navTarget) {
            NavTarget.Explorer -> {
                navController.navigate(AppDestination.Explorer.route) {
                    popUpTo(AppDestination.Splash.route) { inclusive = true }
                }
            }
            NavTarget.Permission -> {
                navController.navigate(AppDestination.Permission.route) {
                    popUpTo(AppDestination.Splash.route) { inclusive = true }
                }
            }
            null -> Unit
        }
    }

    SplashContent()
}

@Composable
private fun SplashContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "StorageTree",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashContentPreview() {
    StorageTreeTheme {
        SplashContent()
    }
}
