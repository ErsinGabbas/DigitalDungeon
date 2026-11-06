package com.example.digitaldungeon.ui.campaign.components

import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The name offered to the system save dialog.
 *
 * The one place in this app where the date is deliberately *not* localised, and the
 * two reasons for that are what is asserted here: ISO order, so a folder full of
 * backups sorts chronologically in any file manager, and a fixed locale, so the digits
 * stay Western and the separators stay legal in a file name.
 *
 * Instants are built through [Calendar], which uses the same default time zone the
 * formatter does, so these assertions hold in every zone.
 */
class BackupFileNameTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    private val sixthOfNovember = at(2026, Calendar.NOVEMBER, 6)

    // ----------------------------------------------------------------- the shape

    @Test
    fun backupFileName_carriesTheDateTheBackupWasTakenOn() {

        assertEquals(
            "digitaldungeon-backup-2026-11-06.json",
            backupFileName(sixthOfNovember)
        )
    }

    @Test
    fun backupFileName_padsMonthAndDayToTwoDigits() {

        // Unpadded, "2026-3-7" would sort after "2026-12-01".
        assertEquals(
            "digitaldungeon-backup-2026-03-07.json",
            backupFileName(at(2026, Calendar.MARCH, 7))
        )
    }

    @Test
    fun backupFileName_isPrefixedSoAFolderOfDownloadsStaysReadable() {

        assertTrue(backupFileName(sixthOfNovember).startsWith("digitaldungeon-backup-"))
    }

    @Test
    fun backupFileName_endsInTheJsonExtension() {

        assertTrue(backupFileName(sixthOfNovember).endsWith(".json"))
    }

    // ---------------------------------------------------------------- it is a date

    @Test
    fun twoBackupsTakenOnTheSameDay_getTheSameName() {

        // The name carries a date, not an instant: the second backup of the day is
        // meant to be offered as a replacement of the first.
        assertEquals(
            backupFileName(at(2026, Calendar.NOVEMBER, 6, hour = 0, minute = 0)),
            backupFileName(at(2026, Calendar.NOVEMBER, 6, hour = 23, minute = 59))
        )
    }

    @Test
    fun backupsTakenOnEitherSideOfMidnight_getDifferentNames() {

        assertNotEquals(
            backupFileName(at(2026, Calendar.NOVEMBER, 6, hour = 23, minute = 59)),
            backupFileName(at(2026, Calendar.NOVEMBER, 7, hour = 0, minute = 0))
        )
    }

    // ------------------------------------------------------------------- sorting

    @Test
    fun namesWithinAYear_sortChronologicallyAsText() {

        val january = backupFileName(at(2026, Calendar.JANUARY, 31))
        val december = backupFileName(at(2026, Calendar.DECEMBER, 1))

        // Day-first or month-first ordering would put the 31st of January last.
        assertTrue(january < december)
    }

    @Test
    fun namesAcrossYears_sortChronologicallyAsText() {

        val endOfYear = backupFileName(at(2025, Calendar.DECEMBER, 31))
        val startOfNext = backupFileName(at(2026, Calendar.JANUARY, 1))

        assertTrue(endOfYear < startOfNext)
    }

    // ------------------------------------------------------ legal as a file name

    @Test
    fun backupFileName_holdsNoCharacterAFileNameMayNotContain() {

        val name = backupFileName(sixthOfNovember)

        // A locale-formatted date could produce slashes, which no file name may hold.
        assertTrue(name.none { it in """/\:*?"<>|""" })
    }

    @Test
    fun backupFileName_usesWesternDigits() {

        assertTrue(
            Regex("""digitaldungeon-backup-\d{4}-\d{2}-\d{2}\.json""")
                .matches(backupFileName(sixthOfNovember))
        )
    }

    /**
     * The fixed [Locale.US] is the point of the formatter, so the name must not move
     * with the device language - a locale whose digits are not Western included.
     */
    @Test
    fun backupFileName_ignoresTheDeviceLocale() {

        val original = Locale.getDefault()

        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))

            assertEquals(
                "digitaldungeon-backup-2026-11-06.json",
                backupFileName(sixthOfNovember)
            )
        } finally {
            Locale.setDefault(original)
        }
    }
}
