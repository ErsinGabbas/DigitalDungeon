package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.campaign.components.CampaignBackupActions
import com.example.digitaldungeon.ui.campaign.components.CampaignListActions
import com.example.digitaldungeon.ui.campaign.state.BackupMessage
import com.example.digitaldungeon.ui.campaign.state.BackupOperation
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The seam between [CampaignUiState] and the campaign list, reachable now that
 * [CampaignScreenContent] exists without `hiltViewModel()` in front of it.
 *
 * The state asks three questions whose wrong answers look much like the right
 * ones on screen, which is what makes them the ones a refactor breaks quietly:
 *
 *  - "nothing has been created" versus "the search hid everything" versus "nothing
 *    is known yet", where the third must claim neither of the first two.
 *  - which of the three dialogs is open when the state somehow carries more than
 *    one of them — there must be exactly one on screen, and it must be the same one
 *    every time.
 *  - whether the backup menu can be opened at all while an operation is running,
 *    which is the only thing standing between an export and a restore racing each
 *    other over the whole database.
 *
 * None of that is visible to the ViewModel tests, which see the state and not the
 * screen, nor to the component tests, which see a card or a menu and not the state.
 */
@RunWith(AndroidJUnit4::class)
class CampaignScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ------------------------------------------------------------------- labels

    private val emptyMessage: String get() = uiString(R.string.campaign_empty)
    private val openLabel: String get() = uiString(R.string.campaign_open_button)
    private val editLabel: String get() = uiString(R.string.common_edit)
    private val deleteLabel: String get() = uiString(R.string.common_delete)

    private val backupMenuLabel: String
        get() = uiString(R.string.backup_menu_content_description)
    private val exportActionLabel: String get() = uiString(R.string.backup_export_action)
    private val restoreActionLabel: String get() = uiString(R.string.backup_restore_action)
    private val dismissMessageLabel: String get() = uiString(R.string.common_dismiss_message)

    private val editDialogTitle: String get() = uiString(R.string.campaign_edit_dialog_title)
    private val deleteDialogTitle: String get() = uiString(R.string.campaign_delete_dialog_title)
    private val restoreWarningTitle: String
        get() = uiString(R.string.backup_restore_warning_title)

    private fun noResultsMessage(query: String): String =
        uiString(R.string.campaign_search_no_results, query)

    // --------------------------------------------------------- recorded callbacks

    private val editRequested = mutableListOf<Campaign>()
    private val deleteRequested = mutableListOf<Campaign>()
    private val opened = mutableListOf<Int>()
    private var exportCalls = 0
    private var restoreCalls = 0
    private var dismissMessageCalls = 0

    private fun showScreen(state: CampaignUiState) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                CampaignScreenContent(
                    state = state,
                    onOpenCampaign = { opened += it },
                    actions = CampaignListActions(
                        onEditCampaignRequest = { editRequested += it },
                        onDeleteCampaignRequest = { deleteRequested += it }
                    ),
                    backupActions = CampaignBackupActions(
                        onExportBackup = { exportCalls++ },
                        onRestoreBackup = { restoreCalls++ },
                        onDismissMessage = { dismissMessageCalls++ }
                    )
                )
            }
        }
    }

    private fun openBackupMenu() {
        composeTestRule.onNodeWithContentDescription(backupMenuLabel).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Brings a campaign card into view. The list of campaigns is the only lazy
     * layout on this screen, which is what `hasScrollToKeyAction()` picks out.
     */
    private fun scrollListTo(text: String) {
        composeTestRule
            .onNode(hasScrollToKeyAction())
            .performScrollToNode(hasText(text))
    }

    /**
     * Every dialog currently on screen, as nodes.
     *
     * "Which dialog is open" is asked of the dialog roots rather than of the
     * wording inside them: a dialog title that is also a label somewhere on the
     * list behind it - which the campaign detail screen genuinely has - makes a
     * text lookup find rows instead of dialogs. Counting dialog roots is also the
     * stronger claim, since it rules out a second dialog this test never thought of.
     */
    private val dialogs get() = composeTestRule.onAllNodes(isDialog())

    /** The node with this text *inside the open dialog*, title or button label. */
    private fun inDialog(text: String) =
        composeTestRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    // ============================================================================
    // Nothing to show, and the three different reasons for it
    // ============================================================================

    @Test
    fun anAppWithoutCampaignsSaysNoneHaveBeenCreatedYet() {
        showScreen(CampaignUiState())

        composeTestRule.onNodeWithText(emptyMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(noResultsMessage(MATCHES_NO_CAMPAIGN)).assertDoesNotExist()
    }

    @Test
    fun aSearchThatHidEveryCampaignNamesWhatWasTypedInstead() {
        showScreen(loadedState.copy(searchQuery = MATCHES_NO_CAMPAIGN))

        composeTestRule
            .onNodeWithText(noResultsMessage(MATCHES_NO_CAMPAIGN))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyMessage).assertDoesNotExist()
    }

    /**
     * Before the first load has come back, nothing at all is known. Saying "no
     * campaigns yet" here would be a guess, and the guess is wrong for every user
     * who has any.
     */
    @Test
    fun theFirstLoadClaimsNeitherAnEmptyListNorAnEmptySearch() {
        showScreen(CampaignUiState(isLoading = true))

        composeTestRule.onNodeWithText(emptyMessage).assertDoesNotExist()
        composeTestRule.onNodeWithText(noResultsMessage("")).assertDoesNotExist()
    }

    @Test
    fun aSearchWithResultsShowsOnlyTheMatchingCampaigns() {
        showScreen(loadedState.copy(searchQuery = MATCHES_THE_SECOND_CAMPAIGN))

        composeTestRule.onNodeWithText(annihilation.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(strahd.name).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(noResultsMessage(MATCHES_THE_SECOND_CAMPAIGN))
            .assertDoesNotExist()
    }

    // ============================================================================
    // The backup menu
    // ============================================================================

    @Test
    fun theBackupMenuRefusesASecondOperationWhileOneIsRunning() {
        showScreen(loadedState.copy(backupOperation = BackupOperation.Restore))

        composeTestRule.onNodeWithContentDescription(backupMenuLabel).assertIsNotEnabled()
    }

    @Test
    fun tappingTheBackupMenuWhileAnOperationRunsOpensNothing() {
        showScreen(loadedState.copy(backupOperation = BackupOperation.Restore))

        openBackupMenu()

        composeTestRule.onNodeWithText(exportActionLabel).assertDoesNotExist()
        composeTestRule.onNodeWithText(restoreActionLabel).assertDoesNotExist()
        assertEquals(0, exportCalls)
        assertEquals(0, restoreCalls)
    }

    @Test
    fun theBackupMenuOffersBothActionsWhileNothingIsRunning() {
        showScreen(loadedState)

        openBackupMenu()

        composeTestRule.onNodeWithText(exportActionLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(restoreActionLabel).assertIsDisplayed()
    }

    @Test
    fun pickingExportAsksForAnExportOnly() {
        showScreen(loadedState)

        openBackupMenu()
        composeTestRule.onNodeWithText(exportActionLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, exportCalls)
        assertEquals("exporting must never start a restore", 0, restoreCalls)
    }

    @Test
    fun pickingRestoreAsksForTheWarningOnly() {
        showScreen(loadedState)

        openBackupMenu()
        composeTestRule.onNodeWithText(restoreActionLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, restoreCalls)
        assertEquals("restoring must never start an export", 0, exportCalls)
    }

    @Test
    fun aRunningOperationNamesItselfAboveTheForm() {
        showScreen(loadedState.copy(backupOperation = BackupOperation.Restore))

        composeTestRule
            .onNodeWithText(uiString(R.string.backup_progress_restore))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(uiString(R.string.backup_progress_export))
            .assertDoesNotExist()
    }

    /**
     * The progress line, the result banner and the inline error are three different
     * channels with three different lifetimes, and a restore that has just finished
     * while the last load failed produces all of them at once. None may push another
     * off the screen.
     */
    @Test
    fun theProgressLineTheResultBannerAndTheLoadErrorAreAllShownAtOnce() {
        showScreen(
            loadedState.copy(
                backupOperation = BackupOperation.Export,
                backupMessage = BackupMessage.error(
                    UiText.of(R.string.backup_restore_not_a_backup)
                ),
                error = UiText.of(R.string.campaign_error_load_failed)
            )
        )

        composeTestRule
            .onNodeWithText(uiString(R.string.backup_progress_export))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(uiString(R.string.backup_restore_not_a_backup))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_error_load_failed))
            .assertIsDisplayed()
    }

    @Test
    fun dismissingTheResultBannerIsReported() {
        showScreen(
            loadedState.copy(
                backupMessage = BackupMessage.error(
                    UiText.of(R.string.backup_restore_not_a_backup)
                )
            )
        )

        composeTestRule.onNodeWithContentDescription(dismissMessageLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissMessageCalls)
    }

    // ============================================================================
    // Dialogs, at most one
    // ============================================================================

    /**
     * All three dialog fields set at once, which the ViewModel never produces. The
     * screen still has to show exactly one — three `if`s instead of one `when` would
     * stack a warning about replacing the whole database on top of a rename.
     */
    @Test
    fun theEditDialogIsTheOnlyOneOnScreenWhenEveryDialogStateIsSet() {
        showScreen(
            loadedState.copy(
                editingCampaign = strahd,
                editName = strahd.name,
                editDescription = strahd.description,
                pendingDeletion = PendingCampaignDeletion(
                    campaignId = annihilation.id,
                    campaignName = annihilation.name
                ),
                isRestoreWarningVisible = true
            )
        )

        dialogs.assertCountEquals(1)
        inDialog(editDialogTitle).assertIsDisplayed()
        inDialog(deleteDialogTitle).assertDoesNotExist()
        inDialog(restoreWarningTitle).assertDoesNotExist()
    }

    @Test
    fun theDeleteConfirmationOutranksTheRestoreWarning() {
        showScreen(
            loadedState.copy(
                pendingDeletion = PendingCampaignDeletion(
                    campaignId = strahd.id,
                    campaignName = strahd.name
                ),
                isRestoreWarningVisible = true
            )
        )

        dialogs.assertCountEquals(1)
        inDialog(deleteDialogTitle).assertIsDisplayed()
        inDialog(restoreWarningTitle).assertDoesNotExist()
    }

    @Test
    fun theRestoreWarningIsShownWhileNoOtherDialogIsOpen() {
        showScreen(loadedState.copy(isRestoreWarningVisible = true))

        dialogs.assertCountEquals(1)
        inDialog(restoreWarningTitle).assertIsDisplayed()
        inDialog(editDialogTitle).assertDoesNotExist()
        inDialog(deleteDialogTitle).assertDoesNotExist()
    }

    @Test
    fun noDialogIsOnScreenWhileNoDialogStateIsSet() {
        showScreen(loadedState)

        dialogs.assertCountEquals(0)
    }

    /** The question has to name what disappears, so it carries the campaign name. */
    @Test
    fun theDeleteConfirmationNamesTheCampaignItIsAbout() {
        showScreen(
            loadedState.copy(
                pendingDeletion = PendingCampaignDeletion(
                    campaignId = annihilation.id,
                    campaignName = annihilation.name
                )
            )
        )

        inDialog(uiString(R.string.campaign_delete_dialog_text, annihilation.name))
            .assertIsDisplayed()
    }

    // ============================================================================
    // A card's buttons report that card's campaign
    //
    // Two cards with three identically labelled buttons each, so the only thing
    // that can tell "Open" from "Open" is which card it sits on. Each button is
    // reached by scrolling its card into view and then taking the first or the last
    // of the matches: the last match is the last *composed* card either way, which
    // keeps the lookup correct whether or not both cards fit on the device at once.
    // ============================================================================

    @Test
    fun openingTheLastCardReportsTheIdOfThatCampaign() {
        showScreen(twoCardState)

        scrollListTo(annihilation.name)
        composeTestRule.onAllNodesWithText(openLabel).onLast().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(annihilation.id), opened)
    }

    @Test
    fun editingTheFirstCardReportsThatCampaignAndNothingElse() {
        showScreen(twoCardState)

        scrollListTo(slimStrahd.name)
        composeTestRule.onAllNodesWithText(editLabel).onFirst().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(slimStrahd), editRequested)
        assertEquals("editing a campaign must never ask for a deletion", 0, deleteRequested.size)
    }

    @Test
    fun deletingTheLastCardReportsThatCampaignAndNothingElse() {
        showScreen(twoCardState)

        scrollListTo(annihilation.name)
        composeTestRule.onAllNodesWithText(deleteLabel).onLast().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(annihilation), deleteRequested)
        assertEquals(emptyList<Campaign>(), editRequested)
    }

    private companion object {

        const val MATCHES_NO_CAMPAIGN = "dragonlance"

        /** Matches the second campaign's description and nothing of the first. */
        const val MATCHES_THE_SECOND_CAMPAIGN = "jungle"

        /** 15 June 2024, 12:00 UTC — fixed, so the created-at lines are reproducible. */
        const val CREATED_AT: Long = 1_718_452_800_000L

        val strahd = Campaign(
            id = 1,
            name = "Curse of Strahd",
            description = "Gothic horror in Barovia",
            createdAt = CREATED_AT,
            participants = listOf(
                EventParticipant(name = "Ireena", level = 4, maxHp = 28)
            )
        )

        val annihilation = Campaign(
            id = 2,
            name = "Tomb of Annihilation",
            description = "A jungle death march",
            createdAt = CREATED_AT
        )

        val loadedState = CampaignUiState(campaigns = listOf(strahd, annihilation))

        /**
         * The same two campaigns without a party, so both cards are short enough to
         * be on screen together on any device — which is what the per-card callback
         * tests below are about.
         */
        val slimStrahd = strahd.copy(participants = emptyList())

        val twoCardState = CampaignUiState(campaigns = listOf(slimStrahd, annihilation))
    }
}
