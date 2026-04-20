package com.thxios.storagetree.ui.navigation

sealed class AppDestination(val route: String) {
    object Permission : AppDestination("permission")
    object Explorer : AppDestination("explorer")
    object Settings : AppDestination("settings")
}
