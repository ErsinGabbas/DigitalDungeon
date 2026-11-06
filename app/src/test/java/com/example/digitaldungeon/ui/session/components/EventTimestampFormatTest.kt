package com.example.digitaldungeon.ui.session.components

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The timestamp on a timeline card.
 *
 * Both tiers come from [DateFormat], so the exact wording is the device locale's
 * business and asserting a literal like "4:12 PM" would only pin the machine the
 * suite happens to run on. What is asserted here is the behaviour that is the
 * function's own: which of the two tiers an instant falls into, that the tier is
 * decided by the calendar day rather than by an interval, and that two instants a
 * reader needs to tell apart come out as different labels.
 *
 * All instants are built through [Calendar], which uses the same default time zone
 * the function does, so the test says the same thing in every zone.
 */
class EventTimestampFormatTest {

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millisecond: Int = 0
    ): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, millisecond)
        }.timeInMillis

    /** How this device spells a bare time - the whole of the "today" tier. */
    private fun timeAlone(millis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

    private val today = at(2026, Calendar.NOVEMBER, 6, hour = 14, minute = 30)

    // --------------------------------------------------------------- the two tiers

    @Test
    fun anEventFromToday_isLabelledWithTheTimeAlone() {

        val timestamp = at(2026, Calendar.NOVEMBER, 6, hour = 9, minute = 5)

        // While the session is being run, "which day" is never the question.
        assertEquals(timeAlone(timestamp), formatEventTimestamp(timestamp, now = today))
    }

    @Test
    fun anEventFromAnEarlierDay_addsTheDateToTheTime() {

        val timestamp = at(2026, Calendar.JUNE, 15, hour = 9, minute = 5)

        val label = formatEventTimestamp(timestamp, now = today)

        // In a log kept over months the date is the actual question.
        assertNotEquals(timeAlone(timestamp), label)
        assertTrue(label.length > timeAlone(timestamp).length)
    }

    @Test
    fun anEventLaterTodayThanNow_isStillLabelledWithTheTimeAlone() {

        val timestamp = at(2026, Calendar.NOVEMBER, 6, hour = 23, minute = 45)

        // The tier is the calendar day, not "already happened".
        assertEquals(timeAlone(timestamp), formatEventTimestamp(timestamp, now = today))
    }

    @Test
    fun anEventAtMidnightOfToday_isLabelledWithTheTimeAlone() {

        val timestamp = at(2026, Calendar.NOVEMBER, 6)

        assertEquals(timeAlone(timestamp), formatEventTimestamp(timestamp, now = today))
    }

    // ------------------------------------------- the day boundary, not a 24h window

    /**
     * The rule is "same calendar day", not "less than 24 hours apart" - the latter
     * would call an event from yesterday evening "today".
     */
    @Test
    fun anEventFromYesterdayEvening_isNotLabelledAsToday() {

        val earlyToday = at(2026, Calendar.NOVEMBER, 6, hour = 1)
        val yesterdayEvening = at(2026, Calendar.NOVEMBER, 5, hour = 23)

        val label = formatEventTimestamp(yesterdayEvening, now = earlyToday)

        // Two hours apart, and still a different day.
        assertNotEquals(timeAlone(yesterdayEvening), label)
    }

    @Test
    fun anEventAMillisecondBeforeMidnight_isNotLabelledAsToday() {

        val midnight = at(2026, Calendar.NOVEMBER, 6)
        val justBefore = at(
            2026, Calendar.NOVEMBER, 5,
            hour = 23, minute = 59, second = 59, millisecond = 999
        )

        assertNotEquals(
            timeAlone(justBefore),
            formatEventTimestamp(justBefore, now = midnight)
        )
    }

    /**
     * Guards the year half of the day comparison: a bare day-of-year match would make
     * an event exactly one year old look like it happened this morning.
     */
    @Test
    fun anEventOnTheSameDayOfAnEarlierYear_isNotLabelledAsToday() {

        val thisYear = at(2026, Calendar.JUNE, 15, hour = 12)
        val lastYear = at(2025, Calendar.JUNE, 15, hour = 12)

        assertNotEquals(
            timeAlone(lastYear),
            formatEventTimestamp(lastYear, now = thisYear)
        )
    }

    // ------------------------------------------------------------ distinguishability

    @Test
    fun twoEventsAtDifferentTimesOfTheSameDay_getDifferentLabels() {

        val morning = at(2026, Calendar.NOVEMBER, 6, hour = 9, minute = 5)
        val evening = at(2026, Calendar.NOVEMBER, 6, hour = 17, minute = 40)

        assertNotEquals(
            formatEventTimestamp(morning, now = today),
            formatEventTimestamp(evening, now = today)
        )
    }

    @Test
    fun twoEventsOnDifferentDaysAtTheSameTime_getDifferentLabels() {

        val june = at(2026, Calendar.JUNE, 15, hour = 9, minute = 5)
        val july = at(2026, Calendar.JULY, 15, hour = 9, minute = 5)

        assertNotEquals(
            formatEventTimestamp(june, now = today),
            formatEventTimestamp(july, now = today)
        )
    }

    // ------------------------------------------------------------------- stability

    @Test
    fun theSameArgumentsAlwaysProduceTheSameLabel() {

        val timestamp = at(2026, Calendar.JUNE, 15, hour = 9, minute = 5)

        // A new DateFormat is built per call; that must not make the output drift.
        assertEquals(
            formatEventTimestamp(timestamp, now = today),
            formatEventTimestamp(timestamp, now = today)
        )
    }

    @Test
    fun nowOnlyDecidesTheTierAndNotTheLabelItself() {

        val timestamp = at(2026, Calendar.NOVEMBER, 6, hour = 9, minute = 5)
        val earlyToday = at(2026, Calendar.NOVEMBER, 6, hour = 6)
        val lateToday = at(2026, Calendar.NOVEMBER, 6, hour = 23, minute = 59)

        // Nothing relative is spelled out, so the label does not move with the clock.
        assertEquals(
            formatEventTimestamp(timestamp, now = earlyToday),
            formatEventTimestamp(timestamp, now = lateToday)
        )
    }
}
