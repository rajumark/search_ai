package com.photo.searchai.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photo.searchai.ui.screens.HomeScreen
import com.photo.searchai.ui.screens.PermissionScreen
import com.photo.searchai.ui.screens.SearchByLabelsScreen
import com.photo.searchai.ui.screens.SearchByTextScreen
import com.photo.searchai.ui.screens.SplashScreen
import com.photo.searchai.ui.screens.ExploreByLabelsScreen

@ExperimentalFoundationApi
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
            HomeScreen(
                    onNavigateToSearch = { navController.navigate("search_by_text") },
                    onNavigateToFavorites = {
                        navController.navigate("search_by_text?search_query=is favorite")
                    },
                    onNavigateToGrouping = { navController.navigate("grouping_by_text") },
                    onNavigateToLabels = { navController.navigate("explore_by_labels") }
            )
        }
        composable(
                route = "search_by_text?search_query={search_query}",
                arguments =
                        listOf(
                                androidx.navigation.navArgument("search_query") {
                                    defaultValue = ""
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { SearchByTextScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("grouping_by_text") {
            com.photo.searchai.ui.screens.GroupingByTextScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupClick = { query ->
                        navController.navigate("search_by_text?search_query=$query")
                    }
            )
        }
        composable("explore_by_labels") {
            ExploreByLabelsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLabelClick = { label ->
                        navController.navigate("search_by_labels?label=$label")
                    }
            )
        }
        composable(
                route = "search_by_labels?label={label}",
                arguments =
                        listOf(
                                androidx.navigation.navArgument("label") {
                                    defaultValue = ""
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) {
            SearchByLabelsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
