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
                onOpenCampaign = { campaignId ->
                    navController.navigate("details/$campaignId")
                }
            )
        }

        composable(
            route = "details/{campaignId}",
            arguments = listOf(
                navArgument("campaignId") { type = NavType.IntType }
            )
        ) {
            // campaignId reaches the ViewModel through SavedStateHandle
            CampaignDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                }
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
