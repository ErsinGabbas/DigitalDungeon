package com.example.digitaldungeon.ui.quest.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.QuestSort

/**
 * The labels a user sees for the quest enums.
 *
 * In the UI layer on purpose, the same way `eventTypeLabelRes` is. The enum constants themselves
 * are storage: they are persisted as text in the `quests` table, written as SQL literals in the
 * Room migration and carried as text in the backup document, so renaming or translating them would
 * break data that is already on disk. Printing `name` instead was the other side of the same coin -
 * it showed the user the storage format ("NOT_STARTED").
 *
 * This file is the single place the two concerns meet, and every `when` is exhaustive, so a new
 * constant cannot be added without giving it a label.
 */
@StringRes
fun questCategoryLabelRes(category: QuestCategory): Int =
    when (category) {
        QuestCategory.MAIN -> R.string.quest_category_main
        QuestCategory.SIDE -> R.string.quest_category_side
        QuestCategory.PERSONAL -> R.string.quest_category_personal
        QuestCategory.FACTION -> R.string.quest_category_faction
        QuestCategory.REPEATABLE -> R.string.quest_category_repeatable
        QuestCategory.OTHER -> R.string.quest_category_other
    }

@StringRes
fun questStatusLabelRes(status: QuestStatus): Int =
    when (status) {
        QuestStatus.NOT_STARTED -> R.string.quest_status_not_started
        QuestStatus.ACTIVE -> R.string.quest_status_active
        QuestStatus.PAUSED -> R.string.quest_status_paused
        QuestStatus.COMPLETED -> R.string.quest_status_completed
        QuestStatus.FAILED -> R.string.quest_status_failed
        QuestStatus.CANCELLED -> R.string.quest_status_cancelled
    }

/**
 * Shorter wording than [questStatusLabelRes] on purpose: these labels sit as three chips inside
 * every objective row, where "Completed" would crowd out the objective itself. "Done" says the
 * same thing in half the space.
 */
@StringRes
fun questObjectiveStatusLabelRes(status: QuestObjectiveStatus): Int =
    when (status) {
        QuestObjectiveStatus.NOT_STARTED -> R.string.quest_objective_status_not_started
        QuestObjectiveStatus.COMPLETED -> R.string.quest_objective_status_completed
        QuestObjectiveStatus.FAILED -> R.string.quest_objective_status_failed
    }

@StringRes
fun questPriorityLabelRes(priority: QuestPriority): Int =
    when (priority) {
        QuestPriority.HIGH -> R.string.quest_priority_high
        QuestPriority.NORMAL -> R.string.quest_priority_normal
        QuestPriority.LOW -> R.string.quest_priority_low
    }

@StringRes
fun questRewardTypeLabelRes(type: QuestRewardType): Int =
    when (type) {
        QuestRewardType.EXPERIENCE -> R.string.quest_reward_type_experience
        QuestRewardType.MONEY -> R.string.quest_reward_type_money
        QuestRewardType.ITEM -> R.string.quest_reward_type_item
        QuestRewardType.REPUTATION -> R.string.quest_reward_type_reputation
        QuestRewardType.OTHER -> R.string.quest_reward_type_other
    }

/**
 * The consequence kinds read as what the change *touches* - "the world", "someone's fate" - rather
 * than as nouns, because that is how the row above them is phrased ("what it touches").
 */
@StringRes
fun questConsequenceTypeLabelRes(type: QuestConsequenceType): Int =
    when (type) {
        QuestConsequenceType.WORLD -> R.string.quest_consequence_type_world
        QuestConsequenceType.NPC -> R.string.quest_consequence_type_npc
        QuestConsequenceType.FACTION -> R.string.quest_consequence_type_faction
        QuestConsequenceType.OTHER -> R.string.quest_consequence_type_other
    }

/**
 * Unlike the five above, [QuestSort] is never stored, so this mapping exists only so the sort row
 * reads the same way the filter rows do.
 */
@StringRes
fun questSortLabelRes(sort: QuestSort): Int =
    when (sort) {
        QuestSort.PRIORITY -> R.string.quest_sort_priority
        QuestSort.NEWEST -> R.string.quest_sort_newest
    }

/** The localized label of this category, for use inside composition. */
@Composable
fun QuestCategory.label(): String = stringResource(questCategoryLabelRes(this))

/** The localized label of this status, for use inside composition. */
@Composable
fun QuestStatus.label(): String = stringResource(questStatusLabelRes(this))

/** The localized label of this priority, for use inside composition. */
@Composable
fun QuestPriority.label(): String = stringResource(questPriorityLabelRes(this))

/** The localized label of this objective status, for use inside composition. */
@Composable
fun QuestObjectiveStatus.label(): String = stringResource(questObjectiveStatusLabelRes(this))

/** The localized label of this reward kind, for use inside composition. */
@Composable
fun QuestRewardType.label(): String = stringResource(questRewardTypeLabelRes(this))

/** The localized label of this consequence kind, for use inside composition. */
@Composable
fun QuestConsequenceType.label(): String = stringResource(questConsequenceTypeLabelRes(this))

/** The localized label of this sort order, for use inside composition. */
@Composable
fun QuestSort.label(): String = stringResource(questSortLabelRes(this))
