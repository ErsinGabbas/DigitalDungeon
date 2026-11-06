package com.example.digitaldungeon.data.backup

/**
 * Full backup of the database as a single JSON document.
 *
 * JSON rather than a copy of the `.db` file on purpose: a file copy breaks as soon as the schema
 * version of the backup and of the app disagree, it drags the WAL along, and nobody can read it.
 * A document survives migrations and can be inspected by eye.
 *
 * The service knows nothing about Android: no `Context`, no files, no permissions. It turns the
 * database into a string and a string back into the database, and the UI decides where that
 * string comes from and goes to.
 */
interface BackupService {

    /**
     * Reads the whole database and renders it as a backup document.
     *
     * @return the snapshot as pretty printed JSON, tagged with [BACKUP_FORMAT_VERSION].
     */
    suspend fun export(): String

    /**
     * Replaces the entire contents of the database with the contents of [json].
     *
     * A restore is a replacement, not a merge: whatever is in the database now is gone
     * afterwards. It happens in one transaction, so either the database ends up being exactly the
     * document or it is left exactly as it was - a half restored database is worse than an
     * untouched one.
     *
     * Never throws on bad input; see [RestoreResult].
     */
    suspend fun restore(json: String): RestoreResult
}
