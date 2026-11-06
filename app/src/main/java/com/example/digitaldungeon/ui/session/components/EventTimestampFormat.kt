package com.example.digitaldungeon.ui.session.components

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * The "when" of a timeline entry, as a short label for its card.
 *
 * Two tiers, because the two readings of a session log need different things:
 *  - an event from the same calendar day as [now] shows only the time
 *    ("4:12 PM"), which is what matters while the session is being run;
 *  - anything older shows date and time ("15 Jun 2024, 4:12 PM"), because in a
 *    log kept over months "which day" is the actual question.
 *
 * Relative wording ([android.text.format.DateUtils.getRelativeTimeSpanString])
 * was deliberately not used: it turns into "3 months ago" exactly where the
 * reader wants a date, and it would pull an Android framework class into what is
 * otherwise a pure function.
 *
 * [now] is a parameter rather than a call to the clock inside, so a caller — a
 * `@Preview` above all — can render a stamp that does not change between runs.
 *
 * Both formats come from [DateFormat], so order, separators and the 12/24 hour
 * choice follow the device locale. The instances are created per call on
 * purpose: [DateFormat] is not thread safe and freezes locale and time zone at
 * construction, so a cached one would keep formatting with the old locale after
 * the user changes it.
 */
internal fun formatEventTimestamp(
    timestamp: Long,
    now: Long
): String {
    val date = Date(timestamp)

    return if (isSameDay(timestamp, now)) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    } else {
        DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(date)
    }
}

/**
 * Same calendar day in the device time zone — not "less than 24 hours apart",
 * which would call an event from yesterday evening "today".
 */
private fun isSameDay(first: Long, second: Long): Boolean {
    val firstDay = Calendar.getInstance().apply { timeInMillis = first }
    val secondDay = Calendar.getInstance().apply { timeInMillis = second }

    return firstDay.get(Calendar.YEAR) == secondDay.get(Calendar.YEAR) &&
        firstDay.get(Calendar.DAY_OF_YEAR) == secondDay.get(Calendar.DAY_OF_YEAR)
}
