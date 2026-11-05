package com.example.digitaldungeon.domain.export

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent

/**
 * Renders a session and its timeline as Markdown.
 *
 * Deliberately a pure function with no Android or Compose dependency: the whole
 * point of the export lives here and can be covered by plain JVM unit tests,
 * while the UI only decides *when* to call it and where to send the result.
 *
 * @param session the session whose log is exported.
 * @param events the events of that session. Ordered defensively by
 *   [SessionEvent.orderIndex] so a caller handing over an unsorted list still
 *   gets the timeline in the order the user sees on screen.
 * @return the complete log as Markdown, without a trailing newline.
 */
fun formatSessionAsMarkdown(
    session: Session,
    events: List<SessionEvent>
): String {
    // Blocks are joined with a blank line at the very end, so no branch has to
    // remember to add (or not add) separators of its own.
    val blocks = mutableListOf<String>()

    blocks += "# " + session.title.trim().ifBlank { UNTITLED_SESSION }

    session.notes.trim().takeIf { it.isNotEmpty() }?.let { notes ->
        blocks += notes
    }

    blocks += "## $TIMELINE_HEADING"

    if (events.isEmpty()) {
        blocks += EMPTY_TIMELINE
    } else {
        events
            .sortedWith(compareBy({ it.orderIndex }, { it.id }))
            .forEachIndexed { index, event ->
                blocks += event.toMarkdownBlocks(position = index + 1)
            }
    }

    return blocks.joinToString(separator = "\n\n")
}

/**
 * One event as a list of Markdown blocks: heading, body and — when the event
 * carries participant snapshots — a labelled bullet list.
 */
private fun SessionEvent.toMarkdownBlocks(position: Int): List<String> {
    val blocks = mutableListOf<String>()

    val eventTitle = title?.trim()?.takeIf { it.isNotEmpty() }
    blocks += buildString {
        append("### ")
        append(position)
        append(". ")
        append(type.name)
        // Quick notes are created without a title, so the heading has to read
        // well with the type alone
        if (eventTitle != null) {
            append(" — ")
            append(eventTitle)
        }
    }

    content.trim().takeIf { it.isNotEmpty() }?.let { body ->
        blocks += body
    }

    if (participants.isNotEmpty()) {
        blocks += "$PARTICIPANTS_LABEL:"
        blocks += participants.joinToString(separator = "\n") { it.toMarkdownBullet() }
    }

    return blocks
}

private fun EventParticipant.toMarkdownBullet(): String =
    "- $name (Lvl $level, HP $maxHp)"

private const val UNTITLED_SESSION = "Untitled session"
private const val TIMELINE_HEADING = "Timeline"
private const val EMPTY_TIMELINE = "_No events recorded._"
private const val PARTICIPANTS_LABEL = "Participants"
