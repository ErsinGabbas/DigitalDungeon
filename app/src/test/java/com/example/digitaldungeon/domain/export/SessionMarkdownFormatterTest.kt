package com.example.digitaldungeon.domain.export

import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.participant
import com.example.digitaldungeon.testing.session
import com.example.digitaldungeon.testing.sessionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Markdown export.
 *
 * Asserted structurally - block by block, heading by heading - rather than against
 * one golden string: a single expected document would break on any cosmetic change
 * and would not say which rule was violated.
 */
class SessionMarkdownFormatterTest {

    /**
     * The document split into its blocks. The formatter joins blocks with a blank
     * line, so this is exactly the list of blocks it produced.
     */
    private fun blocksOf(markdown: String): List<String> =
        markdown.split("\n\n")

    /** Every event heading, in document order. */
    private fun headingsOf(markdown: String): List<String> =
        markdown.lines().filter { it.startsWith("### ") }

    // ------------------------------------------------------------ session title

    @Test
    fun sessionTitle_becomesTheTopLevelHeading() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = emptyList()
        )

        assertEquals("# The Long Night", blocksOf(markdown).first())
    }

    @Test
    fun blankSessionTitle_fallsBackToAPlaceholderHeading() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "   "),
            events = emptyList()
        )

        // An empty "# " heading would render as nothing at all.
        assertEquals("# Untitled session", blocksOf(markdown).first())
    }

    @Test
    fun sessionTitle_isTrimmed() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "  The Long Night  "),
            events = emptyList()
        )

        assertEquals("# The Long Night", blocksOf(markdown).first())
    }

    // ------------------------------------------------------------ session notes

    @Test
    fun sessionNotes_followTheTitle() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = "nobody survived"),
            events = emptyList()
        )

        val blocks = blocksOf(markdown)
        assertEquals("# The Long Night", blocks[0])
        assertEquals("nobody survived", blocks[1])
    }

    @Test
    fun sessionNotes_areTrimmed() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = "\n  nobody survived \n"),
            events = emptyList()
        )

        assertEquals("nobody survived", blocksOf(markdown)[1])
    }

    @Test
    fun emptySessionNotes_produceNoNotesBlock() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = emptyList()
        )

        // The timeline heading follows the title directly, with no blank block in between.
        val blocks = blocksOf(markdown)
        assertEquals("# The Long Night", blocks[0])
        assertEquals("## Timeline", blocks[1])
    }

    @Test
    fun whitespaceOnlySessionNotes_produceNoNotesBlock() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = "   \n  "),
            events = emptyList()
        )

        assertEquals("## Timeline", blocksOf(markdown)[1])
    }

    // -------------------------------------------------------------- empty timeline

    @Test
    fun emptyTimeline_saysSoInsteadOfLeavingTheSectionBlank() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = emptyList()
        )

        val blocks = blocksOf(markdown)
        assertEquals(listOf("# The Long Night", "## Timeline", "_No events recorded._"), blocks)
        assertTrue(headingsOf(markdown).isEmpty())
    }

    @Test
    fun timelineHeading_isAlwaysPresent() {

        val withEvents = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(sessionEvent(id = 1))
        )
        val withoutEvents = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = emptyList()
        )

        assertTrue(withEvents.contains("## Timeline"))
        assertTrue(withoutEvents.contains("## Timeline"))
    }

    // --------------------------------------------------------------- ordering

    @Test
    fun events_areOrderedByOrderIndex() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            // Handed over in the wrong order on purpose.
            events = listOf(
                sessionEvent(id = 30, title = "Third", orderIndex = 2),
                sessionEvent(id = 10, title = "First", orderIndex = 0),
                sessionEvent(id = 20, title = "Second", orderIndex = 1)
            )
        )

        assertEquals(
            listOf("First", "Second", "Third"),
            headingsOf(markdown).map { it.substringAfter("— ") }
        )
    }

    @Test
    fun events_withTheSameOrderIndex_areOrderedById() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(id = 9, title = "Later", orderIndex = 0),
                sessionEvent(id = 4, title = "Earlier", orderIndex = 0)
            )
        )

        assertEquals(
            listOf("Earlier", "Later"),
            headingsOf(markdown).map { it.substringAfter("— ") }
        )
    }

    @Test
    fun events_areNumberedSequentiallyInTheOrderTheyAreRendered() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(id = 30, title = "Third", orderIndex = 7),
                sessionEvent(id = 10, title = "First", orderIndex = 3),
                sessionEvent(id = 20, title = "Second", orderIndex = 5)
            )
        )

        // The position is the place in the log, not the stored orderIndex.
        assertEquals(
            listOf("### 1.", "### 2.", "### 3."),
            headingsOf(markdown).map { it.take(6) }
        )
    }

    // ---------------------------------------------------------- event heading

    @Test
    fun eventHeading_showsPositionTypeAndTitle() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.SCENE,
                    title = "The Tavern",
                    orderIndex = 0
                )
            )
        )

        assertEquals(
            listOf("### 1. SCENE — The Tavern"),
            headingsOf(markdown)
        )
    }

    @Test
    fun eventWithoutTitle_headingIsJustPositionAndType() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            // A quick note is created without a title at all.
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.NOTE,
                    title = null,
                    content = "the bard tips the barkeep",
                    orderIndex = 0
                )
            )
        )

        assertEquals(listOf("### 1. NOTE"), headingsOf(markdown))
        // No dangling separator where the title would have been.
        assertFalse(markdown.contains("—"))
    }

    @Test
    fun eventWithBlankTitle_isTreatedAsHavingNoTitle() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(id = 1, type = SessionEventType.NOTE, title = "   ", orderIndex = 0)
            )
        )

        assertEquals(listOf("### 1. NOTE"), headingsOf(markdown))
    }

    @Test
    fun eventTitle_isTrimmed() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.SCENE,
                    title = "  The Tavern  ",
                    orderIndex = 0
                )
            )
        )

        assertEquals(listOf("### 1. SCENE — The Tavern"), headingsOf(markdown))
    }

    // ---------------------------------------------------------- event content

    @Test
    fun eventContent_followsItsHeading() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.SCENE,
                    title = "The Tavern",
                    content = "the party meets a stranger",
                    orderIndex = 0
                )
            )
        )

        assertEquals(
            listOf(
                "# The Long Night",
                "## Timeline",
                "### 1. SCENE — The Tavern",
                "the party meets a stranger"
            ),
            blocksOf(markdown)
        )
    }

    @Test
    fun eventWithEmptyContent_producesNoContentBlock() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.SCENE,
                    title = "The Tavern",
                    content = "",
                    orderIndex = 0
                )
            )
        )

        assertEquals(
            listOf("# The Long Night", "## Timeline", "### 1. SCENE — The Tavern"),
            blocksOf(markdown)
        )
    }

    @Test
    fun eventWithWhitespaceOnlyContent_producesNoContentBlock() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(id = 1, title = "The Tavern", content = "  \n ", orderIndex = 0),
                sessionEvent(id = 2, title = "The Road", content = "it rains", orderIndex = 1)
            )
        )

        val blocks = blocksOf(markdown)
        // The second event follows the first heading immediately - no blank block.
        assertEquals("### 1. NOTE — The Tavern", blocks[2])
        assertEquals("### 2. NOTE — The Road", blocks[3])
        assertEquals("it rains", blocks[4])
    }

    @Test
    fun eventContent_isTrimmed() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(id = 1, title = "The Tavern", content = "\n  it rains \n", orderIndex = 0)
            )
        )

        assertEquals("it rains", blocksOf(markdown).last())
    }

    // ----------------------------------------------------------- participants

    @Test
    fun eventParticipants_areListedUnderALabel() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.COMBAT,
                    title = "Goblin Ambush",
                    content = "three goblins",
                    orderIndex = 0,
                    participants = listOf(
                        participant(name = "Rogar", level = 4, maxHp = 38),
                        participant(name = "Lyra", level = 3, maxHp = 22)
                    )
                )
            )
        )

        val blocks = blocksOf(markdown)
        assertEquals("Participants:", blocks[blocks.size - 2])
        assertEquals(
            listOf("- Rogar (Lvl 4, HP 38)", "- Lyra (Lvl 3, HP 22)"),
            blocks.last().lines()
        )
    }

    @Test
    fun eventParticipants_keepTheirGivenOrder() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night"),
            events = listOf(
                sessionEvent(
                    id = 1,
                    orderIndex = 0,
                    participants = listOf(
                        participant(name = "Zoe"),
                        participant(name = "Abel")
                    )
                )
            )
        )

        assertEquals(
            listOf("- Zoe (Lvl 3, HP 25)", "- Abel (Lvl 3, HP 25)"),
            blocksOf(markdown).last().lines()
        )
    }

    @Test
    fun eventWithoutParticipants_producesNoParticipantBlock() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(
                    id = 1,
                    type = SessionEventType.COMBAT,
                    title = "Goblin Ambush",
                    content = "three goblins",
                    orderIndex = 0,
                    participants = emptyList()
                )
            )
        )

        assertFalse(markdown.contains("Participants:"))
        assertEquals(
            listOf(
                "# The Long Night",
                "## Timeline",
                "### 1. COMBAT — Goblin Ambush",
                "three goblins"
            ),
            blocksOf(markdown)
        )
    }

    @Test
    fun participants_areListedUnderTheirOwnEventOnly() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(
                    id = 1,
                    title = "With Party",
                    content = "a fight",
                    orderIndex = 0,
                    participants = listOf(participant(name = "Rogar", level = 4, maxHp = 38))
                ),
                sessionEvent(
                    id = 2,
                    title = "Alone",
                    content = "a quiet moment",
                    orderIndex = 1,
                    participants = emptyList()
                )
            )
        )

        assertEquals(
            listOf(
                "# The Long Night",
                "## Timeline",
                "### 1. NOTE — With Party",
                "a fight",
                "Participants:",
                "- Rogar (Lvl 4, HP 38)",
                "### 2. NOTE — Alone",
                "a quiet moment"
            ),
            blocksOf(markdown)
        )
    }

    // --------------------------------------------------------------- document

    @Test
    fun document_hasNoTrailingNewline() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = "nobody survived"),
            events = listOf(sessionEvent(id = 1, content = "something happened", orderIndex = 0))
        )

        assertEquals(markdown.trimEnd(), markdown)
    }

    @Test
    fun document_hasNoBlankBlockAnywhere() {

        val markdown = formatSessionAsMarkdown(
            session = session(title = "The Long Night", notes = ""),
            events = listOf(
                sessionEvent(id = 1, title = null, content = "", orderIndex = 0),
                sessionEvent(id = 2, title = "  ", content = "  ", orderIndex = 1)
            )
        )

        assertTrue(blocksOf(markdown).none { it.isBlank() })
    }
}
