package com.example.digitaldungeon.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * User-facing text produced where the locale is not known.
 *
 * A ViewModel must not hold an already formatted string: the system language can change
 * while the app is alive, and a message baked in at the moment it was produced would keep
 * the old language until it is regenerated. Carrying the resource id and its arguments
 * defers formatting to render time, which is also what makes the messages assertable in
 * unit tests without pulling in resources.
 */
sealed interface UiText {

    /** A localized string, optionally with format arguments. */
    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    /**
     * Text that cannot be localized because it does not come from this app - typically
     * an exception message. Prefer [Resource] everywhere else.
     */
    data class Raw(val value: String) : UiText

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): Resource =
            Resource(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource ->
        if (args.isEmpty()) {
            stringResource(id)
        } else {
            stringResource(id, *args.toTypedArray())
        }

    is UiText.Raw -> value
}

/** Non-composable resolution, for places that already hold a [Context]. */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Resource ->
        if (args.isEmpty()) {
            context.getString(id)
        } else {
            context.getString(id, *args.toTypedArray())
        }

    is UiText.Raw -> value
}
