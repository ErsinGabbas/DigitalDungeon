package com.example.digitaldungeon.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces [Dispatchers.Main] with a [TestDispatcher] for the duration of a test.
 *
 * `viewModelScope` resolves `Dispatchers.Main.immediate` the first time it is touched,
 * and there is no main dispatcher on the JVM - without this rule every ViewModel under
 * test would blow up on its first `viewModelScope.launch`.
 *
 * [testDispatcher] is exposed so the test body can run on the very same scheduler
 * (`runTest(mainDispatcherRule.testDispatcher)`); otherwise `advanceUntilIdle()` would
 * drain a different queue than the one the ViewModel posts to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
