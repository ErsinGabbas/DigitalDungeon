package com.example.digitaldungeon.ui.quest.components

import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER

/**
 * Quests for `@Preview`, shared by the card, the list and the quest screen so that the same quest
 * is recognisable across all of them.
 *
 * Fixed ids and fixed dates, never `System.currentTimeMillis()`: a preview is a reference
 * rendering, and one whose text changes on every pass cannot be compared against anything.
 */
internal val PREVIEW_QUEST = Quest(
    id = 1,
    campaignId = 1,
    title = "Bring Ireena to Vallaki",
    description = "Ismark asks the party to escort his sister past the Tser Pool before nightfall.",
    category = QuestCategory.MAIN,
    status = QuestStatus.ACTIVE,
    priority = QuestPriority.HIGH,
    isHidden = false,
    createdAt = PREVIEW_CREATED_AT_EARLIER,
    inGameDate = "14th of Neth, 735 BC"
)

/**
 * The quietest a quest row gets: no description, no in-world date, nothing but a title - which is
 * exactly what a quest written down in one step looks like.
 */
internal val PREVIEW_QUEST_MINIMAL = Quest(
    id = 2,
    campaignId = 1,
    title = "The miller's missing daughter",
    description = "",
    category = QuestCategory.SIDE,
    status = QuestStatus.NOT_STARTED,
    priority = QuestPriority.NORMAL,
    isHidden = false,
    createdAt = PREVIEW_CREATED_AT,
    inGameDate = null
)

/** Hidden and already failed: the two states that have to be visible at a glance in a long list. */
internal val PREVIEW_QUEST_HIDDEN = Quest(
    id = 3,
    campaignId = 1,
    title = "Strahd's invitation",
    description = "The count will invite the party to dinner once they reach Ravenloft.",
    category = QuestCategory.OTHER,
    status = QuestStatus.FAILED,
    priority = QuestPriority.LOW,
    isHidden = true,
    createdAt = PREVIEW_CREATED_AT,
    inGameDate = null
)

internal val PREVIEW_QUESTS = listOf(
    PREVIEW_QUEST,
    PREVIEW_QUEST_MINIMAL,
    PREVIEW_QUEST_HIDDEN
)
