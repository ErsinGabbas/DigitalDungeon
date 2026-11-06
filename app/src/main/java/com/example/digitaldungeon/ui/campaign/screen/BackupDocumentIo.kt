package com.example.digitaldungeon.ui.campaign.screen

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

/**
 * The half of the backup flow that cannot be moved away from Android: reading and
 * writing the document the user picked.
 *
 * Split out of [CampaignScreen] so that the screen file holds UI only. What is
 * left in the screen is the part that genuinely needs an activity - the two
 * `rememberLauncherForActivityResult` pickers - while these two functions are
 * plain functions over a [ContentResolver] and can be exercised directly from an
 * instrumented test with a provider-backed URI.
 */

/**
 * The document type backups are saved as and filtered by when being picked.
 * Written by [com.example.digitaldungeon.ui.campaign.components.backupFileName]
 * into the extension as well, so the file keeps its type even on a filesystem
 * that has no concept of MIME types.
 */
internal const val BACKUP_MIME_TYPE = "application/json"

/**
 * Writes [text] to the document behind [uri], replacing whatever was there.
 *
 * Opened with mode `"wt"`, not the default `"w"`. Document providers are not
 * required to truncate on `"w"`, so exporting a smaller backup over a larger one
 * would leave the tail of the previous document in place - the file stops being
 * valid JSON and the user only finds out when a restore refuses it.
 *
 * Call from a background dispatcher only - every line of this touches storage
 * through another app's provider.
 *
 * @throws IOException if the provider handed back no stream, which is its way of
 * saying the document cannot be written (deleted, read-only, revoked grant).
 */
internal fun ContentResolver.writeBackupText(uri: Uri, text: String) {

    val stream = openOutputStream(uri, "wt")
        ?: throw IOException("the file could not be opened for writing")

    stream.use { output ->
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(text)
        }
    }
}

/**
 * Reads the document behind [uri] as UTF-8 text. Background dispatcher only.
 *
 * @throws IOException if the provider handed back no stream.
 */
internal fun ContentResolver.readBackupText(uri: Uri): String {

    val stream = openInputStream(uri)
        ?: throw IOException("the file could not be opened for reading")

    return stream.use { input ->
        input.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}
