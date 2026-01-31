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
import com.photo.searchai.ui.fullscreen.FullScreenImageViewer
import com.photo.searchai.ui.fullscreen.FullScreenViewModel
import com.photo.searchai.ui.home.HomeScreen
import com.photo.searchai.ui.home.HomeViewModel
import com.photo.searchai.ui.permission.NavigationEvent
import com.photo.searchai.ui.permission.PermissionScreen
import com.photo.searchai.ui.permission.PermissionViewModel
import com.photo.searchai.ui.search.SearchByTextScreen
import com.photo.searchai.ui.search.SearchViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Permission.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
                    NavigationEvent.None -> { /* No-op */ }
                }
            }
            
            PermissionScreen(viewModel = viewModel)
        }
        
        composable(NavRoutes.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSearch = {
                    navController.navigate(NavRoutes.SearchByText.route)
                }
            )
        }
        
        composable(NavRoutes.SearchByText.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchByTextScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFullScreen = { mediaStoreId, index ->
                    navController.navigate(NavRoutes.FullScreenImage.createRoute(mediaStoreId, index))
                }
            )
        }
        
        composable(
            route = NavRoutes.FullScreenImage.route,
            arguments = listOf(
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

