package com.example.digitaldungeon.data.local.database

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one test whose job is to prove the harness works: real SQLite, on the JVM, no device.
 *
 * Kept after the suite it unblocked, because when a Robolectric upgrade breaks the android-all jar
 * or the SDK level, this is the failure that says so in one line instead of a hundred.
 */
@RunWith(RobolectricTestRunner::class)
class RobolectricSmokeTest {

    private lateinit var database: AppDatabase

    @Before
    fun open() {
        database = createTestDatabase()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun theDatabaseOpensAtTheCurrentVersion_andStoresARow() = runTest {
        database.campaignDao().insertCampaign(
            CampaignEntity(id = 0, name = "Curse of Strahd", description = "", createdAt = 1L)
        )

        assertEquals(
            listOf("Curse of Strahd"),
            database.campaignDao().getAllCampaigns().map { it.name }
        )
    }
}
