package com.example.digitaldungeon.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * An in-memory [AppDatabase] for the JVM tests.
 *
 * Built by hand rather than through Hilt: these tests are about what the DAOs and repositories do
 * to the data, and a graph would only add a way for the test to differ from what it is testing.
 *
 * `allowMainThreadQueries` is on because Robolectric runs the whole test on the main thread; the
 * production code never touches the database from it - every call site is a `suspend` function or a
 * `Flow`.
 *
 * Foreign keys are left to Room, which turns them on for every connection it opens. That matters
 * here: the cascade behaviour these tests assert is the schema's, not the code's, and a test that
 * silently ran without enforcement would pass while the app lost rows.
 */
fun createTestDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    )
        .allowMainThreadQueries()
        .build()
