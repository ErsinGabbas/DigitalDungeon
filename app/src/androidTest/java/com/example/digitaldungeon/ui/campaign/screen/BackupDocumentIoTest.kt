package com.example.digitaldungeon.ui.campaign.screen

import android.content.ContentResolver
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * Reading and writing the document a backup lives in.
 *
 * This is the half of the backup flow that cannot be moved off Android, and it
 * became testable when it was lifted out of the screen's composition into
 * `BackupDocumentIo.kt`: a `ContentResolver` and a URI are all these two functions
 * need, so they can be exercised directly instead of through a system file picker
 * that no instrumented test can drive.
 *
 * What is actually at stake here is not "does it write a file" but two things that
 * would corrupt a user's only copy of their campaigns:
 *
 *  - the charset. The document is JSON holding campaign names, and this app is
 *    read in Russian as well. Written in the platform default encoding and read
 *    back as UTF-8 - or the reverse - a backup would restore mojibake, and the
 *    round trip through one wrong pair of functions still *looks* perfect because
 *    both sides agree with each other. So each direction is checked against bytes
 *    rather than against the other direction.
 *  - truncation. Exporting a smaller database over an older, larger backup must
 *    leave no tail of the old document behind: trailing bytes after the closing
 *    brace turn the file into something the restore will refuse, and the user
 *    finds out at the moment they need it.
 */
@RunWith(AndroidJUnit4::class)
class BackupDocumentIoTest {

    private lateinit var resolver: ContentResolver
    private lateinit var cacheDir: File
    private lateinit var document: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        resolver = context.contentResolver
        cacheDir = context.cacheDir

        // In the cache directory, so a crashed run cannot leave anything behind
        // that matters, and never where the installed app keeps its own data
        document = File.createTempFile("backup-io-test", ".json", cacheDir)
    }

    @After
    fun tearDown() {
        document.delete()
    }

    private val documentUri: Uri get() = Uri.fromFile(document)

    @Test
    fun aBackupIsWrittenToTheDocumentAsUtf8() {
        resolver.writeBackupText(documentUri, BACKUP_WITH_CYRILLIC)

        // The bytes on disk, not what readBackupText makes of them: two functions
        // that share the same wrong charset agree with each other perfectly
        assertEquals(
            BACKUP_WITH_CYRILLIC,
            document.readBytes().toString(Charsets.UTF_8)
        )
    }

    @Test
    fun aUtf8DocumentIsReadBackCharacterForCharacter() {
        document.writeBytes(BACKUP_WITH_CYRILLIC.toByteArray(Charsets.UTF_8))

        assertEquals(BACKUP_WITH_CYRILLIC, resolver.readBackupText(documentUri))
    }

    @Test
    fun writingOverALongerBackupLeavesNoTailOfTheOldOne() {
        val filler = "{\"name\":\"filler\"},".repeat(500)
        val longerBackup = "{\"version\":1,\"campaigns\":[$filler{}]}"

        resolver.writeBackupText(documentUri, longerBackup)
        resolver.writeBackupText(documentUri, SHORT_BACKUP)

        assertEquals(SHORT_BACKUP, resolver.readBackupText(documentUri))
    }

    @Test
    fun readingADocumentThatIsNotThereFailsInsteadOfReturningNothing() {
        val missing = Uri.fromFile(File(cacheDir, "no-such-backup-${System.nanoTime()}.json"))

        // An empty string would be a plausible "backup" to hand the restore, and
        // the restore refuses what it cannot parse - but the user would be told
        // their file is not a backup instead of that it could not be read
        assertThrows(IOException::class.java) {
            resolver.readBackupText(missing)
        }
    }

    private companion object {

        /**
         * Campaign names are user text, and the app is read in Russian as well as
         * English. The newline and the em dash are here for the same reason: they
         * are the characters a charset slip mangles first.
         */
        const val BACKUP_WITH_CYRILLIC =
            "{\"version\":1,\"campaigns\":[{\"name\":\"Проклятие Страда — акт I\"}]}\n"

        const val SHORT_BACKUP = "{\"version\":1,\"campaigns\":[]}"
    }
}
