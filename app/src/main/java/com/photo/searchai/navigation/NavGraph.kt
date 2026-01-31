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
import com.photo.searchai.battery.BatteryOptimizationScreen
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
                    }
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
            FaceSearchScreen(onNavigateBack = { navController.popBackStack() })
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
