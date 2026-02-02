package com.photo.searchai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photo.searchai.app.ui.home.AppHome
import com.photo.searchai.app.ui.onboarding.OnboardingScreen
import com.photo.searchai.core.permissions.ui.PermissionScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Onboarding) {
        composable<Route.Onboarding> {
            OnboardingScreen(onAgreeClick = { navController.navigate(Route.Permission) })
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

        composable<Route.Home> { AppHome() }
    }
}
