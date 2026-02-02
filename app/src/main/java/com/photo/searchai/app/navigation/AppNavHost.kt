package com.photo.searchai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photo.searchai.app.ui.onboarding.OnboardingScreen
import com.photo.searchai.core.permissions.logic.PermissionManager
import com.photo.searchai.core.permissions.ui.PermissionScreen
import com.photo.searchai.feature.home.ui.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager() }

    NavHost(navController = navController, startDestination = Route.Onboarding) {
        composable<Route.Onboarding> {
            OnboardingScreen(
                    onAgreeClick = {
                        val isStorageGranted = permissionManager.isStorageGranted()
                        val isNotificationGranted = permissionManager.isNotificationGranted(context)

                        if (isStorageGranted && isNotificationGranted) {
                            navController.navigate(Route.Home) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Route.Permission)
                        }
                    }
            )
        }

        composable<Route.Permission> {
            PermissionScreen(
                    onAllPermissionsGranted = {
                        // Pop back stack so user can't go back to permission/onboarding
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    }
            )
        }

        composable<Route.Home> { HomeScreen() }
    }
}
