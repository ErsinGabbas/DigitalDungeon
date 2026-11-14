package com.example.digitaldungeon.data.repository

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.dao.QuestRelationDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.QuestRelationEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestRelation
import com.example.digitaldungeon.domain.model.QuestRelationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestRelationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: QuestRelationDao,
    private val questDao: QuestDao
) : QuestRelationRepository {

    override fun getRelationsForQuest(
        questId: Int
    ): Flow<List<QuestRelation>> =
        dao.getRelationsForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addRelation(
        sourceQuestId: Int,
        targetQuestId: Int,
        relationType: QuestRelationType
    ): AddRelationResult {

        // Cheap and independent of the database, so it is answered before a transaction is opened.
        if (sourceQuestId == targetQuestId) return AddRelationResult.SameQuest

        return database.withTransaction {

            val sourceCampaign = questDao.getCampaignIdForQuest(sourceQuestId)
            val targetCampaign = questDao.getCampaignIdForQuest(targetQuestId)

            if (sourceCampaign == null || targetCampaign == null) {
                return@withTransaction AddRelationResult.QuestMissing
            }

            if (sourceCampaign != targetCampaign) {
                return@withTransaction AddRelationResult.DifferentCampaigns
            }

            val normalized = normalize(sourceQuestId, targetQuestId, relationType)

            if (dao.countRelation(normalized.source, normalized.target, normalized.type) > 0) {
                return@withTransaction AddRelationResult.AlreadyExists
            }

            // Only parent links can close a cycle. Two quests that merely precede each other are
            // contradictory rather than circular, and the plan asks for protection against the
            // parent case alone.
            if (normalized.type == QuestRelationType.PARENT_CHILD &&
                wouldCloseCycle(parent = normalized.source, child = normalized.target)
            ) {
                return@withTransaction AddRelationResult.WouldCreateCycle
            }

            dao.insertRelation(
                QuestRelationEntity(
                    sourceQuestId = normalized.source,
                    targetQuestId = normalized.target,
                    relationType = normalized.type
                )
            )

            AddRelationResult.Added
        }
    }

    override suspend fun deleteRelation(
        id: Int
    ) {

        dao.deleteRelation(id)

    }

    /**
     * Whether making [parent] the parent of [child] would make a quest its own ancestor.
     *
     * Answered by walking *up* from [parent]: if [child] is already somewhere above it, then
     * pointing [parent] down at [child] closes the loop. The direct two-quest case - B is already
     * the parent of A, and A is now being made the parent of B - falls out of the same walk, since
     * B is then an ancestor of A.
     *
     * A breadth-first walk and not a single chain, because nothing in the schema says a quest has
     * only one parent: a side quest can honestly belong under two story arcs, so the ancestry is a
     * graph and has to be treated as one.
     *
     * Every parent edge in the table is loaded once rather than queried per step. Relations are a
     * handful of rows per campaign, and one read inside the transaction beats a query per level.
     */
    private suspend fun wouldCloseCycle(parent: Int, child: Int): Boolean {

        val parentEdges = dao.getRelationsOfType(QuestRelationType.PARENT_CHILD)

        // childId -> the quests directly above it
        val parentsOf = parentEdges.groupBy(
            keySelector = { it.targetQuestId },
            valueTransform = { it.sourceQuestId }
        )

        val seen = mutableSetOf(parent)
        val pending = ArrayDeque<Int>().apply { add(parent) }

        while (pending.isNotEmpty()) {

            val current = pending.removeFirst()

            parentsOf[current].orEmpty().forEach { ancestor ->

                if (ancestor == child) return true

                // `seen` is what stops the walk on a cycle that is already in the table - one
                // should not be there, but a walk that trusted that would hang instead of
                // answering.
                if (seen.add(ancestor)) {
                    pending.add(ancestor)
                }
            }
        }

        return false
    }

    /** The row that will actually be written; see `QuestRelationRepository.addRelation`. */
    private data class NormalizedRelation(
        val source: Int,
        val target: Int,
        val type: QuestRelationType
    )

    private fun normalize(
        sourceQuestId: Int,
        targetQuestId: Int,
        relationType: QuestRelationType
    ): NormalizedRelation =
        when (relationType) {

            // The named inverse: stored the other way round so that one fact is one row.
            QuestRelationType.FOLLOWS -> NormalizedRelation(
                source = targetQuestId,
                target = sourceQuestId,
                type = QuestRelationType.PRECEDES
            )

            // Symmetric: the smaller id goes first, so the pair has one canonical form.
            QuestRelationType.ALTERNATIVE,
            QuestRelationType.RELATED -> NormalizedRelation(
                source = minOf(sourceQuestId, targetQuestId),
                target = maxOf(sourceQuestId, targetQuestId),
                type = relationType
            )

            // Directional and already the right way round.
            QuestRelationType.PARENT_CHILD,
            QuestRelationType.PRECEDES -> NormalizedRelation(
                source = sourceQuestId,
                target = targetQuestId,
                type = relationType
            )
        }
}
