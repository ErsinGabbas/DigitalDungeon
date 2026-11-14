package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.database.createTestDatabase
import com.example.digitaldungeon.data.repository.CampaignRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestConsequenceRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestHistoryRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestNoteRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestObjectiveRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRelationRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRewardRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepositoryImpl
import com.example.digitaldungeon.data.repository.SessionRepositoryImpl
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.Session
import kotlinx.coroutines.flow.first

/**
 * The real repositories over a real in-memory database, assembled by hand.
 *
 * The thirteen required scenarios are about what the *data* does - whether two campaigns stay
 * apart, whether a cascade reaches a note three levels down, whether a cycle is refused. A fake
 * repository cannot answer any of those: it would be asserting that the test's own `MutableStateFlow`
 * behaves, which it does. So these tests use the production implementations and let SQLite enforce
 * what SQLite enforces.
 *
 * Built directly rather than through Hilt for the reason spelled out on [createTestDatabase]: a
 * graph here would only add a way for the test's wiring to differ from the app's.
 *
 * Every test that uses this must [close] it, and must be annotated `@RunWith(RobolectricTestRunner)`
 * - the database needs a `Context`.
 */
class QuestTestEnvironment {

    val database: AppDatabase = createTestDatabase()

    val campaigns = CampaignRepositoryImpl(
        database = database,
        dao = database.campaignDao(),
        participantDao = database.campaignParticipantDao()
    )

    val sessions = SessionRepositoryImpl(
        dao = database.sessionDao()
    )

    val quests = QuestRepositoryImpl(
        database = database,
        dao = database.questDao(),
        historyDao = database.questHistoryDao()
    )

    val objectives = QuestObjectiveRepositoryImpl(
        database = database,
        dao = database.questObjectiveDao(),
        historyDao = database.questHistoryDao()
    )

    val rewards = QuestRewardRepositoryImpl(
        dao = database.questRewardDao()
    )

    val consequences = QuestConsequenceRepositoryImpl(
        dao = database.questConsequenceDao()
    )

    val notes = QuestNoteRepositoryImpl(
        dao = database.questNoteDao()
    )

    val relations = QuestRelationRepositoryImpl(
        database = database,
        dao = database.questRelationDao(),
        questDao = database.questDao()
    )

    val history = QuestHistoryRepositoryImpl(
        dao = database.questHistoryDao()
    )

    val sessionLinks = QuestSessionLinkRepositoryImpl(
        database = database,
        dao = database.questSessionLinkDao(),
        questDao = database.questDao(),
        sessionDao = database.sessionDao()
    )

    fun close() = database.close()

    // -------------------------
    // SETUP SHORTHANDS
    //
    // Every repository `add` returns Unit, because the app never needs the new id - the list it
    // just wrote to is a Flow and arrives on its own. A test does need the id, so these wrappers
    // read it back. Reading the whole list and taking the last is safe here and nowhere else: the
    // in-memory database is fresh per test and nothing else is writing.
    // -------------------------

    /** Adds a campaign and answers with its id. */
    suspend fun campaign(name: String = "Curse of Strahd"): Int {
        campaigns.addCampaign(name = name, description = "")
        return campaigns.getCampaigns().first().last { it.name == name }.id
    }

    /** Adds a session to [campaignId] and answers with its id. */
    suspend fun session(campaignId: Int, title: String = "Session 1"): Int {
        sessions.addSession(campaignId = campaignId, title = title, notes = "")
        return sessions.getSessionsForCampaign(campaignId).first().last { it.title == title }.id
    }

    /**
     * Adds a quest with nothing but a campaign and a title - the shape scenario 1 is about - and
     * answers with its id.
     */
    suspend fun quest(campaignId: Int, title: String): Int {
        quests.addQuest(campaignId = campaignId, title = title)
        return quests.getQuestsForCampaign(campaignId).first().first { it.title == title }.id
    }

    /** Adds an objective to [questId] and answers with its id. */
    suspend fun objective(
        questId: Int,
        title: String,
        isOptional: Boolean = false
    ): Int {
        objectives.addObjective(questId = questId, title = title, isOptional = isOptional)
        return objectives.getObjectivesForQuest(questId).first().first { it.title == title }.id
    }

    // -------------------------
    // READ SHORTHANDS
    // -------------------------

    suspend fun questsIn(campaignId: Int): List<Quest> =
        quests.getQuestsForCampaign(campaignId).first()

    suspend fun questTitlesIn(campaignId: Int): List<String> =
        questsIn(campaignId).map { it.title }

    suspend fun questById(id: Int): Quest? =
        quests.getQuestById(id).first()

    suspend fun sessionsIn(campaignId: Int): List<Session> =
        sessions.getSessionsForCampaign(campaignId).first()
}
