package com.example.digitaldungeon.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.encodeParticipants
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Round trip of the whole database through the backup document.
 *
 * This is the one piece of logic that deliberately deletes everything the user owns, so it is
 * exercised against a real Room instance rather than a fake: the transaction, the foreign keys
 * and the JSON column of event participants all have to behave, and none of that is reachable
 * from a JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class BackupServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var service: BackupService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // In-memory, so the test can never disturb the installed app's real journal.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()

        service = BackupServiceImpl(
            database = database,
            campaignDao = database.campaignDao(),
            participantDao = database.campaignParticipantDao(),
            sessionDao = database.sessionDao(),
            eventDao = database.sessionEventDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestore_bringsBackEveryRowUnchanged() = runBlocking {
        seedJournal()

        val document = service.export()

        wipeEverything()
        assertTrue(database.campaignDao().getAllCampaigns().isEmpty())

        val result = service.restore(document)

        assertEquals(
            RestoreResult.Success(
                campaigns = 2,
                campaignParticipants = 2,
                sessions = 2,
                sessionEvents = 2
            ),
            result
        )

        val campaigns = database.campaignDao().getAllCampaigns()
        assertEquals(listOf(1, 2), campaigns.map { it.id })
        assertEquals("Curse of Strahd", campaigns.first { it.id == 1 }.name)

        val roster = database.campaignParticipantDao().getAllParticipants()
        assertEquals(listOf("Ireena", "Ismark"), roster.map { it.name })
        assertEquals(listOf(0, 1), roster.map { it.orderIndex })
        assertTrue(roster.all { it.campaignId == 1 })

        val sessions = database.sessionDao().getAllSessions()
        assertEquals(listOf(10, 11), sessions.map { it.id })
        assertEquals(listOf(1, 2), sessions.map { it.campaignId })
    }

    /**
     * The participant snapshot inside an event is a JSON column, which is exactly the kind of
     * thing a document round trip quietly flattens.
     */
    @Test
    fun exportThenRestore_keepsTheParticipantSnapshotOfAnEvent() = runBlocking {
        seedJournal()

        val document = service.export()
        wipeEverything()
        service.restore(document)

        val events = database.sessionEventDao().getAllEvents()
        val combat = events.single { it.id == 100 }

        assertEquals(SessionEventType.COMBAT, combat.type)
        assertEquals(0, combat.orderIndex)
        assertEquals(
            encodeParticipants(listOf(EventParticipant("Ireena", level = 3, maxHp = 24))),
            combat.participants
        )

        // The quick note had nobody in it, and must not gain anyone on the way back.
        assertEquals(null, events.single { it.id == 101 }.participants)
    }

    @Test
    fun restore_withTextThatIsNotJson_reportsInvalidAndLeavesDataAlone() = runBlocking {
        seedJournal()

        val result = service.restore("этот файл точно не резервная копия")

        assertEquals(RestoreResult.InvalidJson, result)
        assertJournalIsIntact()
    }

    @Test
    fun restore_withJsonThatIsNotABackup_reportsInvalidAndLeavesDataAlone() = runBlocking {
        seedJournal()

        val result = service.restore("""{"something":"else"}""")

        assertEquals(RestoreResult.InvalidJson, result)
        assertJournalIsIntact()
    }

    @Test
    fun restore_withNewerFormatVersion_reportsUnsupportedAndLeavesDataAlone() = runBlocking {
        seedJournal()

        val future = BACKUP_FORMAT_VERSION + 1
        val result = service.restore("""{"formatVersion":$future,"createdAt":0,"campaigns":[]}""")

        assertEquals(
            RestoreResult.UnsupportedFormatVersion(
                formatVersion = future,
                supportedFormatVersion = BACKUP_FORMAT_VERSION
            ),
            result
        )
        assertJournalIsIntact()
    }

    @Test
    fun restore_replacesTheCurrentJournalInsteadOfMergingIntoIt() = runBlocking {
        seedJournal()
        val document = service.export()

        // A campaign that exists only in "now", not in the document.
        database.campaignDao().insertCampaign(
            CampaignEntity(id = 99, name = "Added later", description = "", createdAt = 5L)
        )
        assertEquals(3, database.campaignDao().getAllCampaigns().size)

        service.restore(document)

        val names = database.campaignDao().getAllCampaigns().map { it.name }
        assertEquals(2, names.size)
        assertTrue("restore must not merge, 'Added later' survived", "Added later" !in names)
    }

    @Test
    fun exportOfAnEmptyDatabase_restoresAsEmptyWithoutFailing() = runBlocking {
        val document = service.export()

        val result = service.restore(document)

        assertEquals(
            RestoreResult.Success(
                campaigns = 0,
                campaignParticipants = 0,
                sessions = 0,
                sessionEvents = 0
            ),
            result
        )
        assertTrue(database.campaignDao().getAllCampaigns().isEmpty())
    }

    @Test
    fun exportedDocumentIsReadableText() = runBlocking {
        seedJournal()

        val document = service.export()

        // The whole reason for a JSON document over a copy of the .db file.
        assertTrue(document.contains("Curse of Strahd"))
        assertTrue(document.contains("\"formatVersion\""))
        assertNotEquals("", document.trim())
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun seedJournal() {
        database.campaignDao().insertCampaigns(
            listOf(
                CampaignEntity(id = 1, name = "Curse of Strahd", description = "Barovia", createdAt = 1L),
                CampaignEntity(id = 2, name = "Tomb of Annihilation", description = "", createdAt = 2L)
            )
        )
        database.campaignParticipantDao().insertParticipants(
            listOf(
                CampaignParticipantEntity(id = 1, campaignId = 1, name = "Ireena", level = 3, maxHp = 24, orderIndex = 0),
                CampaignParticipantEntity(id = 2, campaignId = 1, name = "Ismark", level = 4, maxHp = 31, orderIndex = 1)
            )
        )
        database.sessionDao().insertSessions(
            listOf(
                SessionEntity(id = 10, campaignId = 1, title = "Death House", notes = "The party enters", createdAt = 3L),
                SessionEntity(id = 11, campaignId = 2, title = "Port Nyanzaru", notes = "", createdAt = 4L)
            )
        )
        database.sessionEventDao().insertEvents(
            listOf(
                SessionEventEntity(
                    id = 100,
                    sessionId = 10,
                    type = SessionEventType.COMBAT,
                    title = "Shambling mound",
                    content = "The party fled",
                    orderIndex = 0,
                    timestamp = 5L,
                    participants = encodeParticipants(
                        listOf(EventParticipant("Ireena", level = 3, maxHp = 24))
                    )
                ),
                SessionEventEntity(
                    id = 101,
                    sessionId = 10,
                    type = SessionEventType.NOTE,
                    title = null,
                    content = "Remember the raven",
                    orderIndex = 1,
                    timestamp = 6L,
                    participants = null
                )
            )
        )
    }

    private suspend fun wipeEverything() {
        database.sessionEventDao().deleteAllEvents()
        database.sessionDao().deleteAllSessions()
        database.campaignParticipantDao().deleteAllParticipants()
        database.campaignDao().deleteAllCampaigns()
    }

    private suspend fun assertJournalIsIntact() {
        assertEquals(2, database.campaignDao().getAllCampaigns().size)
        assertEquals(2, database.campaignParticipantDao().getAllParticipants().size)
        assertEquals(2, database.sessionDao().getAllSessions().size)
        assertEquals(2, database.sessionEventDao().getAllEvents().size)
    }
}
