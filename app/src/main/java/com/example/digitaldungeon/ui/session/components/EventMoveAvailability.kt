package com.example.digitaldungeon.ui.session.components

/**
 * Whether one reorder arrow of an event card can be used — and when it cannot,
 * *why not*.
 *
 * A `Boolean` was not enough. An arrow is unusable for two unrelated reasons, and
 * the user is owed a different sentence for each:
 *
 *  - [AT_EDGE] — the event is already at that end of the timeline. Self-evident
 *    on screen, but a screen reader has no "already at the top" to look at.
 *  - [FILTER_ACTIVE] — a filter is on, so the neighbour on screen is not the
 *    neighbour in storage and a swap would silently rearrange an order the user
 *    is not being shown. Nothing about the card hints at this, which is exactly
 *    how two dead buttons per card used to appear with no explanation in reach
 *    once the status line above the list had been scrolled away.
 *
 * Carrying the reason instead of a verdict is what lets [SessionEventCard] answer
 * "why is this greyed out" on the card itself, in a tooltip and in the arrow's
 * `contentDescription`, rather than leaving the answer somewhere off screen.
 */
enum class EventMoveAvailability {

    /** The move can be made right now. */
    AVAILABLE,

    /** First event for "up", last event for "down". */
    AT_EDGE,

    /** Reordering is off for the whole timeline while it is filtered. */
    FILTER_ACTIVE;

    val isAvailable: Boolean
        get() = this == AVAILABLE

    companion object {

        /**
         * The verdict for one arrow, in the order the two reasons have to be
         * reported: an active filter outranks the position, because it is the
         * only one of the two the user can do something about and it is the one
         * that is not visible on the card.
         *
         * @param reorderingEnabled `canReorder` of the screen state, i.e. "no
         *   filter is active".
         * @param isAtEdge true when there is no neighbour in this direction.
         */
        fun of(reorderingEnabled: Boolean, isAtEdge: Boolean): EventMoveAvailability =
            when {
                !reorderingEnabled -> FILTER_ACTIVE
                isAtEdge -> AT_EDGE
                else -> AVAILABLE
            }
    }
}
