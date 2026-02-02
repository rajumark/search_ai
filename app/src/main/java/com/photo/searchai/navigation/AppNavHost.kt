package com.photo.searchai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photo.searchai.ui.screens.HomeScreen
import com.photo.searchai.ui.screens.PermissionScreen
import com.photo.searchai.ui.screens.SearchByTextScreen
import com.photo.searchai.ui.screens.SplashScreen

@Composable
fun AppNavHost(
        navController: NavHostController = rememberNavController(),
        startDestination: String = "splash"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(
                    onPermissionsGranted = {
                        navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                    },
                    onPermissionsMissing = {
                        navController.navigate("permission") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
            )
        }
        composable("permission") {
            PermissionScreen(
                    onPermissionsGranted = {
                        navController.navigate("home") {
                            popUpTo("permission") { inclusive = true }
                        }
                    }
            )
        }
        composable("home") {
            HomeScreen(onNavigateToSearch = { navController.navigate("search_by_text") })
        }
        composable("search_by_text") {
            SearchByTextScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
