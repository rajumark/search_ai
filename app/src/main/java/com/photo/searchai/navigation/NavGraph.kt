package com.photo.searchai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.photo.searchai.feature.battery.BatteryOptimizationScreen
import com.photo.searchai.feature.gallery_insights.GalleryInsightsScreen
import com.photo.searchai.feature.gallery_insights.GalleryInsightsViewModel
import com.photo.searchai.feature.media_vault.MediaVaultScreen
import com.photo.searchai.feature.media_vault.MediaVaultViewModel
import com.photo.searchai.feature.onboarding.OnboardingScreen
import com.photo.searchai.feature.smart_albums.SmartAlbumsScreen
import com.photo.searchai.feature.smart_albums.SmartAlbumsViewModel
import com.photo.searchai.feature.storage_cleanup.StorageCleanupScreen
import com.photo.searchai.feature.storage_cleanup.StorageCleanupViewModel
import com.photo.searchai.ui.fullscreen.FullScreenImageViewer
import com.photo.searchai.ui.fullscreen.FullScreenViewModel
import com.photo.searchai.ui.history.RefreshHistoryScreen
import com.photo.searchai.ui.home.HomeScreen
import com.photo.searchai.ui.home.HomeViewModel
import com.photo.searchai.ui.permission.NavigationEvent
import com.photo.searchai.ui.permission.PermissionScreen
import com.photo.searchai.ui.permission.PermissionViewModel
import com.photo.searchai.ui.screens.BarcodePhotosScreen
import com.photo.searchai.ui.screens.DocumentScannerScreen
import com.photo.searchai.ui.screens.FaceSearchScreen
import com.photo.searchai.ui.screens.FaceSearchViewModel
import com.photo.searchai.ui.search.SearchByTextScreen
import com.photo.searchai.ui.search.SearchViewModel

@Composable
fun NavGraph(
        navController: NavHostController,
        startDestination: String = NavRoutes.Permission.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.Permission.route) {
            val viewModel: PermissionViewModel = hiltViewModel()
            val navigationEvent by viewModel.navigationEvent.collectAsState()

            LaunchedEffect(navigationEvent) {
                when (navigationEvent) {
                    is NavigationEvent.NavigateToHome -> {
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.Permission.route) { inclusive = true }
                        }
                        viewModel.onNavigationHandled()
                    }
                    NavigationEvent.None -> {
                        /* No-op */
                    }
                }
            }

            PermissionScreen(viewModel = viewModel)
        }

        composable(NavRoutes.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { navController.navigate(NavRoutes.SearchByText.route) },
                    onNavigateToFaceSearch = { navController.navigate(NavRoutes.FaceSearch.route) },
                    onNavigateToBarcodePhotos = {
                        navController.navigate(NavRoutes.BarcodePhotos.route)
                    },
                    onNavigateToScanner = {
                        navController.navigate(NavRoutes.DocumentScanner.route)
                    },
                    onNavigateToRefreshHistory = {
                        navController.navigate(NavRoutes.RefreshHistory.route)
                    },
                    onNavigateToBatterySettings = {
                        navController.navigate(NavRoutes.BatterySettings.route)
                    },
                    onNavigateToGalleryInsights = {
                        navController.navigate(NavRoutes.GalleryInsights.route)
                    },
                    onNavigateToSmartAlbums = {
                        navController.navigate(NavRoutes.SmartAlbums.route)
                    },
                    onNavigateToStorageCleanup = {
                        navController.navigate(NavRoutes.StorageCleanup.route)
                    },
                    onNavigateToMediaVault = { navController.navigate(NavRoutes.MediaVault.route) }
            )
        }

        composable(NavRoutes.SearchByText.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchByTextScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFullScreen = { mediaStoreId, index ->
                        navController.navigate(
                                NavRoutes.FullScreenImage.createRoute(mediaStoreId, index)
                        )
                    }
            )
        }

        composable(NavRoutes.FaceSearch.route) {
            val viewModel: FaceSearchViewModel = hiltViewModel()
            FaceSearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFullScreen = { mediaStoreId, index ->
                        navController.navigate(
                                NavRoutes.FullScreenImage.createRoute(mediaStoreId, index)
                        )
                    }
            )
        }

        composable(NavRoutes.BarcodePhotos.route) {
            BarcodePhotosScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.DocumentScanner.route) {
            DocumentScannerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.RefreshHistory.route) {
            RefreshHistoryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.BatterySettings.route) {
            BatteryOptimizationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.GalleryInsights.route) {
            val viewModel: GalleryInsightsViewModel = hiltViewModel()
            GalleryInsightsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SmartAlbums.route) {
            val viewModel: SmartAlbumsViewModel = hiltViewModel()
            SmartAlbumsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAlbumClick = { /* TODO: Navigate to album detail */},
                    onCreateAlbumClick = { /* TODO: Navigate to rule editor */}
            )
        }

        composable(NavRoutes.StorageCleanup.route) {
            val viewModel: StorageCleanupViewModel = hiltViewModel()
            StorageCleanupScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.MediaVault.route) {
            val viewModel: MediaVaultViewModel = hiltViewModel()
            MediaVaultScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }

        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                    onAgreeClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(NavRoutes.Home.route) {
                                popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
            )
        }

        composable(
                route = NavRoutes.FullScreenImage.route,
                arguments =
                        listOf(
                                navArgument("mediaStoreId") { type = NavType.StringType },
                                navArgument("initialIndex") { type = NavType.StringType }
                        )
        ) {
            val viewModel: FullScreenViewModel = hiltViewModel()
            FullScreenImageViewer(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
