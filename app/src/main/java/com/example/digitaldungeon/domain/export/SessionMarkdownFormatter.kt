package com.example.digitaldungeon.domain.export

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import java.util.Locale

/**
 * The wording of an exported log.
 *
 * The export is a document the user hands to another person — usually the same person
 * who reads the app — so its headings belong in the app's language. Resolving them is
 * *not* this layer's job though: [formatSessionAsMarkdown] stays a pure function with
 * no Android dependency, and receives the already resolved labels instead of a
 * `Context`. The UI builds this object from string resources
 * (`sessionMarkdownLabels()`), tests either take the defaults or state the labels they
 * assert on.
 *
 * The defaults are the English wording the formatter used before localization, which
 * is what keeps the JVM tests of this file independent of resources.
 *
 * @param participantLine format of a participant bullet: `%1$s` name, `%2$d` level,
 *   `%3$d` max HP. A format string rather than three fragments, because the order of
 *   the parts is a property of the language.
 * @param eventTypeNames how each [SessionEventType] is spelled out. Missing entries
 *   fall back to the enum constant, so an incomplete map degrades instead of crashing.
 */
data class SessionMarkdownLabels(
    val untitledSession: String = "Untitled session",
    val timelineHeading: String = "Timeline",
    val emptyTimeline: String = "_No events recorded._",
    val participantsLabel: String = "Participants",
    val participantLine: String = "%1\$s (Lvl %2\$d, HP %3\$d)",
    val eventTypeNames: Map<SessionEventType, String> =
        SessionEventType.entries.associateWith { it.name }
) {
    internal fun nameOf(type: SessionEventType): String =
        eventTypeNames[type] ?: type.name
}

/**
 * Renders a session and its timeline as Markdown.
 *
 * Deliberately a pure function with no Android or Compose dependency: the whole
 * point of the export lives here and can be covered by plain JVM unit tests,
 * while the UI only decides *when* to call it, in which words ([labels]) and
 * where to send the result.
 *
 * @param session the session whose log is exported.
 * @param events the events of that session. Ordered defensively by
 *   [SessionEvent.orderIndex] so a caller handing over an unsorted list still
 *   gets the timeline in the order the user sees on screen.
 * @param labels the wording of the document; see [SessionMarkdownLabels].
 * @return the complete log as Markdown, without a trailing newline.
 */
fun formatSessionAsMarkdown(
    session: Session,
    events: List<SessionEvent>,
    labels: SessionMarkdownLabels = SessionMarkdownLabels()
): String {
    // Blocks are joined with a blank line at the very end, so no branch has to
    // remember to add (or not add) separators of its own.
    val blocks = mutableListOf<String>()

    blocks += "# " + session.title.trim().ifBlank { labels.untitledSession }

    session.notes.trim().takeIf { it.isNotEmpty() }?.let { notes ->
        blocks += notes
    }

    blocks += "## ${labels.timelineHeading}"

    if (events.isEmpty()) {
        blocks += labels.emptyTimeline
    } else {
        events
            .sortedWith(compareBy({ it.orderIndex }, { it.id }))
            .forEachIndexed { index, event ->
                blocks += event.toMarkdownBlocks(position = index + 1, labels = labels)
            }
    }

    return blocks.joinToString(separator = "\n\n")
}

/**
 * One event as a list of Markdown blocks: heading, body and — when the event
 * carries participant snapshots — a labelled bullet list.
 */
private fun SessionEvent.toMarkdownBlocks(
    position: Int,
    labels: SessionMarkdownLabels
): List<String> {
    val blocks = mutableListOf<String>()

    val eventTitle = title?.trim()?.takeIf { it.isNotEmpty() }
    blocks += buildString {
        append("### ")
        append(position)
        append(". ")
        append(labels.nameOf(type))
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
        blocks += "${labels.participantsLabel}:"
        blocks += participants.joinToString(separator = "\n") {
            it.toMarkdownBullet(labels.participantLine)
        }
    }

    return blocks
}

/**
 * The locale is passed explicitly: the numbers in a bullet have to be rendered the way
 * the rest of the document is, and an implicit default is easy to lose sight of.
 */
private fun EventParticipant.toMarkdownBullet(format: String): String =
    "- " + String.format(Locale.getDefault(), format, name, level, maxHp)
