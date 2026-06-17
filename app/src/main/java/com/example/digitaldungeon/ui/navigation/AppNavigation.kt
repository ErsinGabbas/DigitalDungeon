package com.example.digitaldungeon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digitaldungeon.ui.campaign.screen.CampaignDetailScreen
import com.example.digitaldungeon.ui.campaign.screen.CampaignScreen
import com.example.digitaldungeon.ui.session.screen.SessionDetailScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "campaigns"
    ) {

        composable("campaigns") {
            CampaignScreen(
                navController = navController
            )
        }

        composable("details/{campaignId}") {

            val campaignId =
                it.arguments
                    ?.getString("campaignId")
                    ?.toIntOrNull()
                    ?: 0

            CampaignDetailScreen(
                campaignId = campaignId,
                navController = navController
            )
        }

        composable(
            route = "session/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.IntType }
            )
        ) {
            SessionDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
