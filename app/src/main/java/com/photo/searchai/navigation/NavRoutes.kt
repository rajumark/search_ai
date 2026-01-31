package com.photo.searchai.navigation

/** Navigation routes for the app. */
sealed class NavRoutes(val route: String) {
    data object Permission : NavRoutes("permission")
    data object Home : NavRoutes("home")
    data object SearchByText : NavRoutes("search_by_text")
    data object FaceSearch : NavRoutes("face_search")
    data object BarcodePhotos : NavRoutes("barcode_photos")
    data object DocumentScanner : NavRoutes("document_scanner")
    data object RefreshHistory : NavRoutes("refresh_history")
    data object BatterySettings : NavRoutes("battery_settings")
    data object FullScreenImage : NavRoutes("full_screen_image/{mediaStoreId}/{initialIndex}") {
        fun createRoute(mediaStoreId: Long, initialIndex: Int = 0) =
                "full_screen_image/$mediaStoreId/$initialIndex"
    }
}
