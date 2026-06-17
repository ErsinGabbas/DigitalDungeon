package com.example.digitaldungeon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digitaldungeon.ui.campaign.screen.CampaignDetailScreen
import com.example.digitaldungeon.ui.campaign.screen.CampaignScreen

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
                campaignId = campaignId
            )
        }
    }
}
