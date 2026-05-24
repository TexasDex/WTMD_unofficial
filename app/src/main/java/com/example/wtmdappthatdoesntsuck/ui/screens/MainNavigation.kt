package com.example.wtmdappthatdoesntsuck.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.navigation.navArgument
import androidx.navigation.NavType

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "recent") {
        composable("recent") {
            RecentSongsScreen(
                onNavigateToLiked = { navController.navigate("liked") },
                onNavigateToSettings = { highlight ->
                    val route = if (highlight != null) "settings?highlight=$highlight" else "settings"
                    navController.navigate(route)
                }
            )
        }
        composable("liked") {
            LikedSongsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { highlight ->
                    val route = if (highlight != null) "settings?highlight=$highlight" else "settings"
                    navController.navigate(route)
                }
            )
        }
        composable(
            "settings?highlight={highlight}",
            arguments = listOf(navArgument("highlight") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val highlight = backStackEntry.arguments?.getString("highlight")
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                highlightSection = highlight
            )
        }
    }
}
