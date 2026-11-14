package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus

/**
 * A quest row, owned by exactly one campaign.
 *
 * The campaign is named by a real foreign key rather than by a plain column, so "quests of two
 * campaigns cannot mix" is a property of the schema and not of the queries: every read is
 * `WHERE campaignId = :campaignId`, and a row pointing at a campaign that does not exist cannot be
 * written in the first place. `CASCADE` for the same reason the sessions use it - deleting a
 * campaign must not leave its quests behind as unreachable rows.
 *
 * One index, on [campaignId], because that is the only column any query filters by: the status and
 * category filters are applied in memory to the already loaded list (see `QuestListUiState`), so an
 * index covering them would be dead weight that every insert still pays for.
 *
 * The three enums are stored by Room as TEXT holding the constant name, exactly like
 * `session_events.type`, which is why no `@TypeConverters` appear anywhere in this database.
 */
@Entity(
    tableName = "quests",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["campaignId"])
    ]
)
data class QuestEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val campaignId: Int,

    val title: String,

    val description: String,

    val category: QuestCategory,

    val status: QuestStatus,

    val priority: QuestPriority,

    val isHidden: Boolean,

    val createdAt: Long,

    /** Nullable because most quests are never dated in world time; see `Quest.inGameDate`. */
    val inGameDate: String? = null
)
