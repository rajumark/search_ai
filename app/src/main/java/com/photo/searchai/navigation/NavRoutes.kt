package com.photo.searchai.navigation

/**
 * Navigation routes for the app.
 */
sealed class NavRoutes(val route: String) {
    data object Permission : NavRoutes("permission")
    data object Home : NavRoutes("home")
}
