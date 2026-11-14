package com.example.digitaldungeon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digitaldungeon.ui.campaign.screen.CampaignDetailScreen
import com.example.digitaldungeon.ui.campaign.screen.CampaignScreen
import com.example.digitaldungeon.ui.quest.screen.QuestDetailScreen
import com.example.digitaldungeon.ui.quest.screen.QuestEditScreen
import com.example.digitaldungeon.ui.quest.screen.QuestListScreen
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
                },
                onOpenQuests = { campaignId ->
                    navController.navigate("quests/$campaignId")
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
                onBack = { navController.popBackStack() },
                // The other half of the crossing: from a session to a quest it touched, and from
                // that quest back to its sessions.
                onOpenQuest = { questId ->
                    navController.navigate("quest/$questId")
                }
            )
        }

        // The quest list of one campaign. The campaign is part of the route and not a filter
        // applied later: it is what every query on this screen is scoped by, so two campaigns'
        // quests can never end up in the same list.
        composable(
            route = "quests/{campaignId}",
            arguments = listOf(
                navArgument("campaignId") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            // Read here and not inside the screen, because only the graph knows where "new quest"
            // leads: the route declares the argument as IntType, so it is always present - a
            // missing one would be a programming error, not a campaign number 0.
            val campaignId = checkNotNull(backStackEntry.arguments).getInt("campaignId")

            QuestListScreen(
                onBack = { navController.popBackStack() },
                onOpenQuest = { questId ->
                    navController.navigate("quest/$questId")
                },
                onCreateQuest = {
                    navController.navigate("quest/new/$campaignId")
                }
            )
        }

        composable(
            route = "quest/{questId}",
            arguments = listOf(
                navArgument("questId") { type = NavType.IntType }
            )
        ) {
            QuestDetailScreen(
                onBack = { navController.popBackStack() },
                onEditQuest = { questId ->
                    navController.navigate("quest/$questId/edit")
                },
                // A linked quest is pushed rather than replacing this one, so walking down a
                // story arc and backing out again retraces the way it was walked.
                onOpenQuest = { questId ->
                    navController.navigate("quest/$questId")
                },
                onOpenSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                }
            )
        }

        // Two routes into the same screen: one carries the campaign a new quest belongs to, the
        // other the quest being changed. Which argument is present is what the form reads its mode
        // off, which is why they are separate routes rather than one with an optional id.
        composable(
            route = "quest/new/{campaignId}",
            arguments = listOf(
                navArgument("campaignId") { type = NavType.IntType }
            )
        ) {
            QuestEditScreen(
                onBack = { navController.popBackStack() },
                // Back to the list, which already shows the new quest - it is the newest, so it is
                // at the top under either sort order.
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = "quest/{questId}/edit",
            arguments = listOf(
                navArgument("questId") { type = NavType.IntType }
            )
        ) {
            QuestEditScreen(
                onBack = { navController.popBackStack() },
                // Back to the quest, which observes its own row and therefore already shows what
                // was just written.
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
