package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.Flow

interface QuestRepository {

    /**
     * The quests of one campaign, in a stable order (newest first).
     *
     * Searching, filtering and sorting are not arguments here: they are decisions about one
     * screen's current view of the list, they change on every keystroke, and re-querying the
     * database for each of them would throw away the one thing the unfiltered list is needed
     * for - telling "this campaign has no quests" apart from "nothing matched".
     */
    fun getQuestsForCampaign(
        campaignId: Int
    ): Flow<List<Quest>>

    fun getQuestById(
        id: Int
    ): Flow<Quest?>

    /**
     * Writes down a new quest for [campaignId].
     *
     * Everything but the campaign and the title has a default, because that is the promise the
     * module makes: a quest nobody has thought through yet is one line of text, and the fields it
     * was not given are the ones a DM would have had to answer arbitrarily. [QuestStatus.NOT_STARTED]
     * is the honest starting point, and [QuestCategory.SIDE] the common case - the one tap it costs
     * to change is cheaper than making a category mandatory.
     */
    suspend fun addQuest(
        campaignId: Int,
        title: String,
        description: String = "",
        category: QuestCategory = QuestCategory.SIDE,
        status: QuestStatus = QuestStatus.NOT_STARTED,
        priority: QuestPriority = QuestPriority.NORMAL,
        isHidden: Boolean = false,
        inGameDate: String? = null
    )

    /**
     * Rewrites the editable fields of a quest.
     *
     * Every field is passed every time - the write replaces them as a whole - so a caller that
     * only means to change the title still hands back the rest as it found them. The campaign a
     * quest belongs to and the moment it was created are not editable; see `QuestDao.updateQuest`.
     */
    suspend fun updateQuest(
        id: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    )

    /**
     * Moves a quest to [status] and leaves everything else alone.
     *
     * Any status can be reached from any other, including out of [QuestStatus.COMPLETED] and
     * [QuestStatus.FAILED]: reopening a quest the party thought was over is an ordinary turn of a
     * campaign, so the repository does not police the transition.
     */
    suspend fun updateQuestStatus(
        id: Int,
        status: QuestStatus
    )

    suspend fun deleteQuest(
        id: Int
    )
}
