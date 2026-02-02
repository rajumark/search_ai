package com.photo.searchai.app.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable data object Onboarding : Route()

    @Serializable data object Permission : Route()

    @Serializable data object Home : Route()
}
