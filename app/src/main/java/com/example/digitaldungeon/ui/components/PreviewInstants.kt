package com.example.digitaldungeon.ui.components

/**
 * Fixed instants for `@Preview` data, shared so that every preview showing a
 * creation date shows the same one.
 *
 * Constants and never `System.currentTimeMillis()`: a preview is a reference
 * rendering, and one whose text changes on every pass cannot be compared against
 * anything - neither by eye nor by a screenshot test.
 *
 * Noon UTC rather than midnight, so the date reads the same whichever side of the
 * date line the previewing machine sits on.
 */

/** 2026-11-06, 12:00 UTC. */
internal const val PREVIEW_CREATED_AT = 1_793_966_400_000L

/** 2026-01-01, 12:00 UTC - an older sibling, to show two dates next to each other. */
internal const val PREVIEW_CREATED_AT_EARLIER = 1_767_268_800_000L
