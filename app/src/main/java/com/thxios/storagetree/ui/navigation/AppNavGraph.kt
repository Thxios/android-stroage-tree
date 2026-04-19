package com.thxios.storagetree.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thxios.storagetree.ui.explorer.ExplorerScreen
import com.thxios.storagetree.ui.explorer.ExplorerViewModel
import com.thxios.storagetree.ui.permission.PermissionScreen
import com.thxios.storagetree.ui.permission.PermissionViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Permission.route
    ) {
        composable(AppDestination.Permission.route) {
            val viewModel: PermissionViewModel = hiltViewModel()
            val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
            PermissionScreen(
                hasPermission = hasPermission,
                onRequestPermission = { viewModel.requestPermission(navController.context as android.app.Activity) },
                onNavigateToExplorer = {
                    navController.navigate(AppDestination.Explorer.route) {
                        popUpTo(AppDestination.Permission.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Explorer.route) {
            val viewModel: ExplorerViewModel = hiltViewModel()
            ExplorerScreen(viewModel = viewModel, navController = navController)
        }
    }
}
