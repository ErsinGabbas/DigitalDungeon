package com.example.digitaldungeon.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.encodeParticipants
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.first
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
            eventDao = database.sessionEventDao(),
            questDao = database.questDao(),
            objectiveDao = database.questObjectiveDao(),
            rewardDao = database.questRewardDao(),
            consequenceDao = database.questConsequenceDao(),
            noteDao = database.questNoteDao(),
            relationDao = database.questRelationDao(),
            historyDao = database.questHistoryDao(),
            sessionLinkDao = database.questSessionLinkDao()
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
                sessionEvents = 2,
                quests = 2,
                questObjectives = 3
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

    /**
     * The quests are the youngest thing in the document, and the one a restore would destroy
     * silently if the format had been left alone: deleting the campaigns cascades to them, so a
     * backup that did not carry them back would wipe the quest tracker with no way to recover it.
     */
    @Test
    fun exportThenRestore_bringsBackEveryQuestFieldUnchanged() = runBlocking {
        seedJournal()

        val document = service.export()
        wipeEverything()
        assertTrue(database.questDao().getAllQuests().isEmpty())

        service.restore(document)

        val quests = database.questDao().getAllQuests()
        assertEquals(listOf(200, 201), quests.map { it.id })
        // Both belong to the first campaign; a quest that came back under the wrong one would
        // still add up to the right total.
        assertTrue(quests.all { it.campaignId == 1 })

        val escort = quests.single { it.id == 200 }
        assertEquals("Escort Ireena to Vallaki", escort.title)
        assertEquals("Ismark asks for an escort", escort.description)
        assertEquals(QuestCategory.MAIN, escort.category)
        assertEquals(QuestStatus.ACTIVE, escort.status)
        assertEquals(QuestPriority.HIGH, escort.priority)
        assertEquals(false, escort.isHidden)
        assertEquals(7L, escort.createdAt)
        assertEquals("14th of Neth, 735 BC", escort.inGameDate)

        val invitation = quests.single { it.id == 201 }
        assertEquals(QuestStatus.NOT_STARTED, invitation.status)
        assertEquals(true, invitation.isHidden)
        // Absent in the file, and it must not gain a value on the way back.
        assertEquals(null, invitation.inGameDate)
    }

    /**
     * The objectives are nested two levels deep, which is exactly where a document round trip
     * loses things: under the wrong quest, or in the wrong order.
     */
    @Test
    fun exportThenRestore_bringsBackEveryObjectiveUnderItsOwnQuest() = runBlocking {
        seedJournal()

        val document = service.export()
        wipeEverything()
        assertTrue(database.questObjectiveDao().getAllObjectives().isEmpty())

        service.restore(document)

        val objectives = database.questObjectiveDao().getAllObjectives()
        assertEquals(3, objectives.size)
        // All three belong to the first quest; the second quest has none and must gain none.
        assertTrue(objectives.all { it.questId == 200 })

        val reached = objectives.single { it.id == 300 }
        assertEquals("Reach the Tser Pool", reached.title)
        assertEquals("before nightfall", reached.description)
        assertEquals(QuestObjectiveStatus.COMPLETED, reached.status)
        assertEquals(0, reached.orderIndex)
        assertEquals(false, reached.isOptional)

        val vistani = objectives.single { it.id == 302 }
        assertEquals(QuestObjectiveStatus.NOT_STARTED, vistani.status)
        assertEquals(true, vistani.isOptional)
        // The gap at 5 is carried verbatim: the document stores positions, it does not renumber.
        assertEquals(5, vistani.orderIndex)

        assertEquals(QuestObjectiveStatus.FAILED, objectives.single { it.id == 301 }.status)
    }

    @Test
    fun exportThenRestore_keepsTheOrderTheStepsAreReadIn() = runBlocking {
        seedJournal()

        val document = service.export()
        wipeEverything()
        service.restore(document)

        // Position order, not id order: 300 (0), 301 (1), 302 (5).
        assertEquals(
            listOf("Reach the Tser Pool", "Get Ireena past the gates", "Hear out the Vistani"),
            database.questObjectiveDao().getObjectivesForQuest(200).first().map { it.title }
        )
    }

    /**
     * A backup written by the version that had quests but not yet their steps.
     *
     * Read on purpose: bumping the format must never turn a copy somebody already has into an
     * unreadable file. Such a document simply carries no objectives.
     */
    @Test
    fun restore_ofAFormatVersion2Document_succeedsWithQuestsButNoObjectives() = runBlocking {
        seedJournal()

        val documentV2 = """
            {
              "formatVersion": 2,
              "createdAt": 1700000000000,
              "campaigns": [
                {
                  "id": 1,
                  "name": "Curse of Strahd",
                  "description": "Barovia",
                  "createdAt": 1,
                  "participants": [],
                  "sessions": [],
                  "quests": [
                    {
                      "id": 200,
                      "title": "Escort Ireena to Vallaki",
                      "description": "",
                      "category": "MAIN",
                      "status": "ACTIVE",
                      "priority": "HIGH",
                      "isHidden": false,
                      "createdAt": 7
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = service.restore(documentV2)

        assertEquals(
            RestoreResult.Success(
                campaigns = 1,
                campaignParticipants = 0,
                sessions = 0,
                sessionEvents = 0,
                quests = 1,
                questObjectives = 0
            ),
            result
        )
        assertEquals(
            listOf("Escort Ireena to Vallaki"),
            database.questDao().getAllQuests().map { it.title }
        )
        assertTrue(database.questObjectiveDao().getAllObjectives().isEmpty())
    }

    @Test
    fun restore_replacesTheObjectivesInsteadOfMergingIntoThem() = runBlocking {
        seedJournal()
        val document = service.export()

        database.questObjectiveDao().insertObjective(
            QuestObjectiveEntity(
                id = 399,
                questId = 200,
                title = "Added later",
                description = "",
                status = QuestObjectiveStatus.NOT_STARTED,
                orderIndex = 9,
                isOptional = false
            )
        )

        service.restore(document)

        val titles = database.questObjectiveDao().getAllObjectives().map { it.title }
        assertEquals(3, titles.size)
        assertTrue("restore must not merge, 'Added later' survived", "Added later" !in titles)
    }

    @Test
    fun restore_replacesTheQuestsInsteadOfMergingIntoThem() = runBlocking {
        seedJournal()
        val document = service.export()

        database.questDao().insertQuest(
            QuestEntity(
                id = 299,
                campaignId = 1,
                title = "Added later",
                description = "",
                category = QuestCategory.SIDE,
                status = QuestStatus.NOT_STARTED,
                priority = QuestPriority.NORMAL,
                isHidden = false,
                createdAt = 9L
            )
        )

        service.restore(document)

        val titles = database.questDao().getAllQuests().map { it.title }
        assertEquals(2, titles.size)
        assertTrue("restore must not merge, 'Added later' survived", "Added later" !in titles)
    }

    /**
     * A backup written before the quest tracker existed.
     *
     * Still readable on purpose: bumping the document format must not turn the copies people
     * already have into unreadable files. Such a file simply carries no quests - the field defaults
     * to empty - and everything else comes back exactly as it did before.
     */
    @Test
    fun restore_ofAFormatVersion1Document_succeedsWithoutQuests() = runBlocking {
        seedJournal()

        val legacyDocument = """
            {
              "formatVersion": 1,
              "createdAt": 1700000000000,
              "campaigns": [
                {
                  "id": 1,
                  "name": "Curse of Strahd",
                  "description": "Barovia",
                  "createdAt": 1,
                  "participants": [
                    { "id": 1, "name": "Ireena", "level": 3, "maxHp": 24, "orderIndex": 0 }
                  ],
                  "sessions": [
                    {
                      "id": 10,
                      "title": "Death House",
                      "notes": "The party enters",
                      "createdAt": 3,
                      "events": [
                        {
                          "id": 100,
                          "type": "COMBAT",
                          "content": "The party fled",
                          "orderIndex": 0,
                          "timestamp": 5,
                          "participants": []
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = service.restore(legacyDocument)

        assertEquals(
            RestoreResult.Success(
                campaigns = 1,
                campaignParticipants = 1,
                sessions = 1,
                sessionEvents = 1,
                quests = 0
            ),
            result
        )
        assertEquals(listOf("Curse of Strahd"), database.campaignDao().getAllCampaigns().map { it.name })
        // The replacement still happened in full: the seeded quests belonged to the database that
        // was replaced, not to the file.
        assertTrue(database.questDao().getAllQuests().isEmpty())
    }

    @Test
    fun exportedDocument_isTaggedWithTheCurrentFormatVersion() = runBlocking {
        val document = service.export()

        // The version an older build reads first and refuses on; it has to be the one we write.
        assertTrue(document.contains("\"formatVersion\": $BACKUP_FORMAT_VERSION"))
        assertTrue(BACKUP_FORMAT_VERSION in SUPPORTED_BACKUP_FORMAT_VERSIONS)
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
                sessionEvents = 0,
                quests = 0,
                questObjectives = 0
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
        seedCampaignsSessionsAndEvents()
        seedQuests()
        seedObjectives()
    }

    /**
     * Three steps under the first quest and none under the second, so that grouping by quest is
     * exercised: a step landing under the wrong quest would still add up to the right total.
     *
     * Deliberately not in position order, and with a gap at 5, which is what a list looks like
     * after a deletion. The document has to carry the positions as they are.
     */
    private suspend fun seedObjectives() {
        database.questObjectiveDao().insertObjectives(
            listOf(
                QuestObjectiveEntity(
                    id = 300,
                    questId = 200,
                    title = "Reach the Tser Pool",
                    description = "before nightfall",
                    status = QuestObjectiveStatus.COMPLETED,
                    orderIndex = 0,
                    isOptional = false
                ),
                QuestObjectiveEntity(
                    id = 302,
                    questId = 200,
                    title = "Hear out the Vistani",
                    description = "",
                    status = QuestObjectiveStatus.NOT_STARTED,
                    orderIndex = 5,
                    isOptional = true
                ),
                QuestObjectiveEntity(
                    id = 301,
                    questId = 200,
                    title = "Get Ireena past the gates",
                    description = "",
                    status = QuestObjectiveStatus.FAILED,
                    orderIndex = 1,
                    isOptional = false
                )
            )
        )
    }

    private suspend fun seedCampaignsSessionsAndEvents() {
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

    /**
     * Two quests in the first campaign and none in the second, so that grouping by campaign is
     * actually exercised: a quest landing in the wrong campaign on the way back would still add up
     * to the right total.
     */
    private suspend fun seedQuests() {
        database.questDao().insertQuests(
            listOf(
                QuestEntity(
                    id = 200,
                    campaignId = 1,
                    title = "Escort Ireena to Vallaki",
                    description = "Ismark asks for an escort",
                    category = QuestCategory.MAIN,
                    status = QuestStatus.ACTIVE,
                    priority = QuestPriority.HIGH,
                    isHidden = false,
                    createdAt = 7L,
                    inGameDate = "14th of Neth, 735 BC"
                ),
                QuestEntity(
                    id = 201,
                    campaignId = 1,
                    title = "Strahd's invitation",
                    description = "",
                    category = QuestCategory.OTHER,
                    status = QuestStatus.NOT_STARTED,
                    priority = QuestPriority.LOW,
                    isHidden = true,
                    createdAt = 8L,
                    inGameDate = null
                )
            )
        )
    }

    private suspend fun wipeEverything() {
        database.sessionEventDao().deleteAllEvents()
        database.sessionDao().deleteAllSessions()
        database.questObjectiveDao().deleteAllObjectives()
        database.questDao().deleteAllQuests()
        database.campaignParticipantDao().deleteAllParticipants()
        database.campaignDao().deleteAllCampaigns()
    }

    private suspend fun assertJournalIsIntact() {
        assertEquals(2, database.campaignDao().getAllCampaigns().size)
        assertEquals(2, database.campaignParticipantDao().getAllParticipants().size)
        assertEquals(2, database.sessionDao().getAllSessions().size)
        assertEquals(2, database.sessionEventDao().getAllEvents().size)
        assertEquals(2, database.questDao().getAllQuests().size)
        assertEquals(3, database.questObjectiveDao().getAllObjectives().size)
    }
}
