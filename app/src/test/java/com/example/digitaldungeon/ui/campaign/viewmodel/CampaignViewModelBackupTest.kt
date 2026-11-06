package com.example.digitaldungeon.ui.campaign.viewmodel

import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.backup.RestoreResult
import com.example.digitaldungeon.testing.FakeBackupService
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.ui.campaign.state.BackupMessage
import com.example.digitaldungeon.ui.campaign.state.BackupOperation
import com.example.digitaldungeon.ui.text.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Exporting and restoring the whole database.
 *
 * The ViewModel never touches a file: the screen owns the picker and hands the two
 * halves of the file access over as suspending lambdas. That is what makes both flows
 * testable here, and it is also where the rules worth guarding live - "saved" may only
 * be claimed once the write lambda has returned, the warning has to come before the
 * picker, and a cancelled screen must not be mistaken for a failed backup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignViewModelBackupTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeCampaignRepository
    private lateinit var backupService: FakeBackupService

    /** What the screen was asked to write, in order. Stands in for the file. */
    private val written = mutableListOf<String>()

    @Before
    fun setUp() {
        repository = FakeCampaignRepository()
        backupService = FakeBackupService()
    }

    private fun TestScope.createViewModel(): CampaignViewModel =
        CampaignViewModel(repository, backupService).also { advanceUntilIdle() }

    // ------------------------------------------------------------------ idle

    @Test
    fun beforeAnyBackupOperation_theMenuIsEnabledAndNothingIsRunning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val state = createViewModel().uiState.value

            assertNull(state.backupOperation)
            assertFalse(state.isBackupRunning)
            assertTrue(state.isBackupMenuEnabled)
            assertNull(state.backupMessage)
        }

    // ---------------------------------------------------------------- export

    @Test
    fun exportBackup_handsTheRenderedDocumentToTheWriteLambda() =
        runTest(mainDispatcherRule.testDispatcher) {

            val document = """{"formatVersion":1,"createdAt":5,"campaigns":[]}"""
            backupService.exportJson = document

            val viewModel = createViewModel()

            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            assertEquals(listOf(document), written)
        }

    @Test
    fun exportBackup_onSuccess_reportsHowManyCampaignsWereSaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value =
                listOf(campaign(id = 1), campaign(id = 2), campaign(id = 3))

            val viewModel = createViewModel()

            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            // The count travels as a count; the plural form is the screen's business.
            assertEquals(
                BackupMessage.ExportSucceeded(campaignCount = 3),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_onSuccess_endsTheOperation() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.backupOperation)
            assertFalse(state.isBackupRunning)
            assertTrue(state.isBackupMenuEnabled)
        }

    @Test
    fun exportBackup_whileRunning_disablesTheBackupMenu() =
        runTest(mainDispatcherRule.testDispatcher) {

            val write = CompletableDeferred<Unit>()
            val viewModel = createViewModel()

            viewModel.exportBackup { written += it; write.await() }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(BackupOperation.Export, state.backupOperation)
            assertTrue(state.isBackupRunning)
            assertFalse(state.isBackupMenuEnabled)

            write.complete(Unit)
            advanceUntilIdle()
        }

    /**
     * "Backup saved" may only be claimed once the bytes are in the file: the write is
     * part of the operation, not a step after it.
     */
    @Test
    fun exportBackup_claimsSuccessOnlyAfterTheWriteLambdaHasReturned() =
        runTest(mainDispatcherRule.testDispatcher) {

            val write = CompletableDeferred<Unit>()
            val viewModel = createViewModel()

            viewModel.exportBackup { written += it; write.await() }
            advanceUntilIdle()

            // The document has been handed over, but the file is not written yet.
            assertEquals(1, written.size)
            assertNull(viewModel.uiState.value.backupMessage)

            write.complete(Unit)
            advanceUntilIdle()

            assertEquals(
                BackupMessage.ExportSucceeded(campaignCount = 0),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_whenTheWriteLambdaThrows_reportsAFailedExport() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { throw IllegalStateException("no space left on device") }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_export_failed, "no space left on device"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_whenTheWriteLambdaThrows_doesNotLeaveTheOperationRunning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { throw IllegalStateException("no space left on device") }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.backupOperation)
            assertTrue(state.isBackupMenuEnabled)
        }

    @Test
    fun exportBackup_whenTheBackupServiceFails_reportsAFailedExport() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.exportError = IllegalStateException("could not read the database")

            val viewModel = createViewModel()

            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_export_failed, "could not read the database"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_whenTheBackupServiceFails_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.exportError = IllegalStateException("could not read the database")

            val viewModel = createViewModel()

            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            // A half rendered document must never reach the file the user picked.
            assertTrue(written.isEmpty())
        }

    /**
     * File access throws messageless exceptions all the time (a provider that handed
     * back no stream, a revoked permission), and "Export failed: null" explains nothing.
     */
    @Test
    fun exportBackup_whenTheFailureHasNoMessage_namesTheExceptionInstead() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { throw IllegalStateException() }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_export_failed, "IllegalStateException"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_whenTheFailureHasABlankMessage_namesTheExceptionInstead() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { throw IllegalStateException("   ") }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_export_failed, "IllegalStateException"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun exportBackup_clearsTheMessageOfThePreviousOperation() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.exportBackup { throw IllegalStateException("no space left on device") }
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.backupMessage is BackupMessage.Text)

            val write = CompletableDeferred<Unit>()
            viewModel.exportBackup { write.await() }
            advanceUntilIdle()

            // The banner of the failed attempt must not sit next to the new progress bar.
            assertNull(viewModel.uiState.value.backupMessage)

            write.complete(Unit)
            advanceUntilIdle()
        }

    // --------------------------------------------------- one operation at a time

    /**
     * The menu is disabled while an operation runs, but the guard in the ViewModel is
     * what makes that true: a click queued before the menu went grey must not get
     * through either.
     *
     * The export has to be *in flight* for the guard to bite, hence the
     * `advanceUntilIdle` before the second call - `isBackupRunning` is raised inside
     * the coroutine (on `Dispatchers.Main.immediate` in production, which raises it
     * synchronously).
     */
    @Test
    fun exportBackup_whileAnExportIsRunning_isIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {

            val write = CompletableDeferred<Unit>()
            val viewModel = createViewModel()

            viewModel.exportBackup { written += "first"; write.await() }
            advanceUntilIdle()

            viewModel.exportBackup { written += "second" }
            advanceUntilIdle()

            assertEquals(listOf("first"), written)
            assertEquals(1, backupService.exportCallCount)

            write.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun restoreBackup_whileAnExportIsRunning_isIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {

            val write = CompletableDeferred<Unit>()
            val viewModel = createViewModel()

            viewModel.exportBackup { write.await() }
            advanceUntilIdle()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            // Racing a restore against an export would run two whole-database
            // operations over each other.
            assertTrue(backupService.restoreCalls.isEmpty())

            write.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun restoreBackup_whileARestoreIsRunning_isIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {

            val read = CompletableDeferred<String>()
            val viewModel = createViewModel()

            viewModel.restoreBackup { read.await() }
            advanceUntilIdle()

            viewModel.restoreBackup { """{"second":true}""" }
            advanceUntilIdle()

            assertTrue(backupService.restoreCalls.isEmpty())

            read.complete("""{"first":true}""")
            advanceUntilIdle()

            assertEquals(listOf("""{"first":true}"""), backupService.restoreCalls)
        }

    // ----------------------------------------------------------- cancellation

    /**
     * A cancelled `viewModelScope` means the screen is gone, so there is nobody left to
     * report to. Turning that into "Export failed: Job was cancelled" is the classic
     * mistake a blanket `catch (e: Exception)` makes.
     */
    @Test
    fun exportBackup_whenTheWriteIsCancelled_reportsNoFailure() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.exportBackup { throw CancellationException("the screen is gone") }
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.backupMessage)
        }

    @Test
    fun restoreBackup_whenTheReadIsCancelled_reportsNoFailure() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { throw CancellationException("the screen is gone") }
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.backupMessage)
        }

    // ------------------------------------------------------- restore warning

    @Test
    fun requestRestoreBackup_showsTheWarning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestRestoreBackup()

            assertTrue(viewModel.uiState.value.isRestoreWarningVisible)
        }

    /**
     * The warning comes *before* the file picker. Picking a file and only then being
     * told that everything is about to be overwritten is the wrong order to learn
     * that in - and backing out of the picker then changes nothing at all.
     */
    @Test
    fun requestRestoreBackup_touchesNoData() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestRestoreBackup()
            advanceUntilIdle()

            assertTrue(backupService.restoreCalls.isEmpty())
            assertNull(viewModel.uiState.value.backupOperation)
        }

    @Test
    fun dismissRestoreWarning_hidesTheWarningWithoutRestoringAnything() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestRestoreBackup()

            viewModel.dismissRestoreWarning()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRestoreWarningVisible)
            assertTrue(backupService.restoreCalls.isEmpty())
        }

    @Test
    fun restoreBackup_hidesTheWarning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestRestoreBackup()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRestoreWarningVisible)
        }

    @Test
    fun requestRestoreBackup_clearsThePreviousBackupMessage() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            viewModel.requestRestoreBackup()

            assertNull(viewModel.uiState.value.backupMessage)
        }

    // ------------------------------------------- mutually exclusive dialogs

    @Test
    fun requestRestoreBackup_closesTheEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(campaign(id = 11, name = "Curse of Strahd"))
            viewModel.onEditNameChange("half typed name")

            viewModel.requestRestoreBackup()

            val state = viewModel.uiState.value
            assertNull(state.editingCampaign)
            assertEquals("", state.editName)
            assertTrue(state.isRestoreWarningVisible)
        }

    @Test
    fun requestRestoreBackup_clearsAPendingDeletion() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 99, name = "doomed"))

            viewModel.requestRestoreBackup()

            assertNull(viewModel.uiState.value.pendingDeletion)
            assertTrue(viewModel.uiState.value.isRestoreWarningVisible)
        }

    @Test
    fun requestEditCampaign_hidesTheRestoreWarning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestRestoreBackup()

            viewModel.requestEditCampaign(campaign(id = 11, name = "Curse of Strahd"))

            assertFalse(viewModel.uiState.value.isRestoreWarningVisible)
        }

    @Test
    fun requestDeleteCampaign_hidesTheRestoreWarning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestRestoreBackup()

            viewModel.requestDeleteCampaign(campaign(id = 99, name = "doomed"))

            assertFalse(viewModel.uiState.value.isRestoreWarningVisible)
        }

    // --------------------------------------------------------------- restore

    @Test
    fun restoreBackup_handsTheDocumentItReadToTheBackupService() =
        runTest(mainDispatcherRule.testDispatcher) {

            val document = """{"formatVersion":1,"createdAt":5,"campaigns":[]}"""
            val viewModel = createViewModel()

            viewModel.restoreBackup { document }
            advanceUntilIdle()

            assertEquals(listOf(document), backupService.restoreCalls)
        }

    @Test
    fun restoreBackup_onSuccess_reportsWhatCameBackPerTable() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.restoreResult = RestoreResult.Success(
                campaigns = 2,
                campaignParticipants = 5,
                sessions = 7,
                sessionEvents = 11
            )

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            // "25 records" alone would not tell anyone whether their sessions came back.
            assertEquals(
                BackupMessage.RestoreSucceeded(
                    totalRecords = 25,
                    campaigns = 2,
                    participants = 5,
                    sessions = 7,
                    events = 11
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun restoreBackup_onSuccess_endsTheOperation() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.backupOperation)
            assertTrue(state.isBackupMenuEnabled)
        }

    @Test
    fun restoreBackup_whileRunning_disablesTheBackupMenu() =
        runTest(mainDispatcherRule.testDispatcher) {

            val read = CompletableDeferred<String>()
            val viewModel = createViewModel()

            viewModel.restoreBackup { read.await() }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(BackupOperation.Restore, state.backupOperation)
            assertTrue(state.isBackupRunning)
            assertFalse(state.isBackupMenuEnabled)

            read.complete("""{"formatVersion":1}""")
            advanceUntilIdle()
        }

    @Test
    fun restoreBackup_withAFileThatIsNotABackup_saysSo() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.restoreResult = RestoreResult.InvalidJson

            val viewModel = createViewModel()

            viewModel.restoreBackup { "a shopping list, not a backup" }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_restore_not_a_backup),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun restoreBackup_withAnUnsupportedFormatVersion_namesBothVersions() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.restoreResult = RestoreResult.UnsupportedFormatVersion(
                formatVersion = 9,
                supportedFormatVersion = 1
            )

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":9}""" }
            advanceUntilIdle()

            // The two numbers are arguments, not part of the sentence: a translation
            // decides where in it they go.
            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_restore_unsupported_version, 9, 1),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun restoreBackup_withAnUnsupportedFormatVersion_isReportedAsAFailure() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.restoreResult = RestoreResult.UnsupportedFormatVersion(
                formatVersion = 9,
                supportedFormatVersion = 1
            )

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":9}""" }
            advanceUntilIdle()

            // A refused restore is not a successful one, however gently it is worded.
            assertTrue(viewModel.uiState.value.backupMessage!!.isError)
        }

    @Test
    fun restoreBackup_onSuccess_isNotReportedAsAFailure() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.backupMessage!!.isError)
        }

    @Test
    fun restoreBackup_whenTheFileCannotBeRead_reportsAFailedRestore() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { throw IllegalStateException("permission denied") }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_restore_read_failed, "permission denied"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    /** Letting someone pick a file by hand means unreadable files are ordinary input. */
    @Test
    fun restoreBackup_whenTheFileCannotBeRead_touchesNoData() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { throw IllegalStateException("permission denied") }
            advanceUntilIdle()

            assertTrue(backupService.restoreCalls.isEmpty())
        }

    @Test
    fun restoreBackup_whenTheFileCannotBeRead_doesNotLeaveTheOperationRunning() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { throw IllegalStateException("permission denied") }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.backupOperation)
            assertTrue(state.isBackupMenuEnabled)
        }

    @Test
    fun restoreBackup_whenTheBackupServiceThrows_reportsAFailedRestore() =
        runTest(mainDispatcherRule.testDispatcher) {

            backupService.restoreError = IllegalStateException("the transaction rolled back")

            val viewModel = createViewModel()

            viewModel.restoreBackup { """{"formatVersion":1}""" }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(
                        R.string.backup_restore_read_failed,
                        "the transaction rolled back"
                    ),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    @Test
    fun restoreBackup_whenTheReadFailsWithoutAMessage_namesTheExceptionInstead() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.restoreBackup { throw SecurityException() }
            advanceUntilIdle()

            assertEquals(
                BackupMessage.Text(
                    text = UiText.of(R.string.backup_restore_read_failed, "SecurityException"),
                    isError = true
                ),
                viewModel.uiState.value.backupMessage
            )
        }

    // -------------------------------------------------- dismissing the banner

    @Test
    fun dismissBackupMessage_takesTheBannerOffTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.exportBackup { written += it }
            advanceUntilIdle()
            assertEquals(
                BackupMessage.ExportSucceeded(campaignCount = 0),
                viewModel.uiState.value.backupMessage
            )

            viewModel.dismissBackupMessage()

            assertNull(viewModel.uiState.value.backupMessage)
        }

    @Test
    fun dismissBackupMessage_leavesTheCampaignListAlone() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(campaign(id = 1), campaign(id = 2))
            repository.campaigns.value = stored

            val viewModel = createViewModel()
            viewModel.exportBackup { written += it }
            advanceUntilIdle()

            viewModel.dismissBackupMessage()

            assertEquals(stored, viewModel.uiState.value.campaigns)
        }
}
