package com.example.digitaldungeon.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Reads the user-facing strings out of the same resources the UI reads them from.
 *
 * Every Compose test below looks its nodes up by what is on screen, and what is on
 * screen depends on the device language: a test comparing against the literal
 * "Delete" passes on an English device and fails on a Russian one, for the same
 * correct app. Resolving through the target context keeps the assertions about
 * *which* string is shown rather than about its English spelling.
 */
internal val targetContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

/** The localized value of [id], exactly as a composable's `stringResource` would read it. */
internal fun uiString(@StringRes id: Int): String = targetContext.getString(id)

/** The localized value of [id] with its placeholders filled, for format resources. */
internal fun uiString(@StringRes id: Int, vararg formatArgs: Any): String =
    targetContext.getString(id, *formatArgs)

/**
 * The localized value of the plural [id] for [quantity], with its placeholders filled.
 *
 * A helper of its own because a plural is picked by the rules of the *language*: English
 * has two forms, Russian four, and which one a count takes is not something a test may
 * decide for itself. Resolving it through the same resources the screen uses keeps the
 * assertion about "the sentence for this count" instead of about one language's grammar.
 *
 * @param quantity the number the grammatical form is chosen by — not necessarily one of
 *   [formatArgs], and for `session_filter_status` deliberately the *total* rather than
 *   the visible count.
 */
internal fun uiPlural(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String =
    targetContext.resources.getQuantityString(id, quantity, *formatArgs)
