package com.example.digitaldungeon.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The palette is taken from the launcher icon, so the app and its icon read as
 * one product. The icon's own colors, measured as a share of its area and
 * converted to CIELAB (L* lightness, C chroma, H hue angle):
 *
 * | share | what it is in the drawing          | sRGB    | L*   | C    | H   |
 * |-------|------------------------------------|---------|------|------|-----|
 * | 49%   | leather of the binding, warm brown | #643928 | 28.9 | 25.4 |  47 |
 * | 14%   | blue-grey slate background         | #2B3548 | 22.0 | 13.2 | 276 |
 * | 10%   | turquoise glow of the magic        | #527582 | 47.1 | 14.2 | 231 |
 * |  9%   | gold tooling, parchment, the d20   | #BD9F71 | 67.1 | 28.5 |  81 |
 * |  -    | near-black behind the frame        | #06050A |  1.5 |  2.0 | 298 |
 *
 * Every value below is a tone of one of five families built on those hues, so
 * the whole scheme is four icon hues plus a red the icon does not contain but
 * the semantics demand:
 *
 *  - leather, H 56 - primary. The hue is nudged 9 degrees off the measured 47
 *    towards orange on purpose: red sits at H 22-40 in Lab, and a leather at
 *    H 47 would collide with the error family on the event cards.
 *  - blood, H 22 - error. A wine-tinged crimson rather than the stock Material
 *    #BA1A1A fire red: cooler, and far enough from the leather hue to stay its
 *    own color at every tone.
 *  - slate, H 278 - secondary, straight off the icon's background.
 *  - glow, H 227 - tertiary, the turquoise light around the d20.
 *  - parchment / void, H 80 / H 295 - the neutrals. Gold is 9% of the icon and
 *    belongs to the paper of the journal rather than to an accent: it is the
 *    warm off-white of the light theme, the sepia of surfaceVariant, and the
 *    gold-grey of outline and onSurfaceVariant in the dark theme. The dark
 *    surfaces are the near-black blue-violet from behind the icon's frame.
 *
 * Tones follow the Material 3 convention (light: accent L* 40-44, container
 * L* 79-90, "on" L* 15-27; dark: accent L* 78-80, container L* 25-31, "on"
 * L* 83-91), with two deliberate departures, both driven by SessionEventCard,
 * which paints its five event types with five container roles:
 *
 *  1. primaryContainer in the light theme is a tone 79 tan rather than the
 *     usual tone 90 tint - at tone 90 it would be a pale cream a few steps from
 *     the parchment surfaceVariant and the pink errorContainer.
 *  2. surfaceVariant in the light theme is tone 86, not tone 90, so a NOTE card
 *     is still visible against the parchment page (dE2000 9.4 instead of 3.1).
 *
 * The five containers are at least dE2000 13.2 apart in the light theme and
 * 14.9 in the dark one, and every container/content pair clears WCAG AA: the
 * worst of the ten pairs is 7.1:1, and no role pair in either scheme is below
 * 5.5:1.
 */

// --- Light theme: primary, the leather of the binding ---
val TomeLeatherLight = Color(0xFF875131)
val OnTomeLeatherLight = Color(0xFFFFFFFF)
val TomeLeatherContainerLight = Color(0xFFF2B795)
val OnTomeLeatherContainerLight = Color(0xFF411B00)

// --- Light theme: secondary, the slate behind the tome ---
val DungeonSlateLight = Color(0xFF4D638D)
val OnDungeonSlateLight = Color(0xFFFFFFFF)
val DungeonSlateContainerLight = Color(0xFFD0DDFF)
val OnDungeonSlateContainerLight = Color(0xFF05284C)

// --- Light theme: tertiary, the turquoise glow of the d20 ---
val ArcaneGlowLight = Color(0xFF007289)
val OnArcaneGlowLight = Color(0xFFFFFFFF)
val ArcaneGlowContainerLight = Color(0xFFA3E1F5)
val OnArcaneGlowContainerLight = Color(0xFF002F3A)

// --- Light theme: error, dried blood rather than stock fire red ---
val BloodErrorLight = Color(0xFFB63143)
val OnBloodErrorLight = Color(0xFFFFFFFF)
val BloodErrorContainerLight = Color(0xFFFFDAD9)
val OnBloodErrorContainerLight = Color(0xFF590014)

// --- Light theme: neutrals, warm parchment instead of white ---
val ParchmentLight = Color(0xFFFDF6ED)
val OnParchmentLight = Color(0xFF2B2214)
val ParchmentVariantLight = Color(0xFFE7D5BD)
val OnParchmentVariantLight = Color(0xFF4E3D24)
val OutlineLight = Color(0xFF8A7A64)
val OutlineVariantLight = Color(0xFFD9CAB6)

// Surface container ladder. Not optional decoration: a plain `Card` resolves its
// background to `surfaceContainerHighest` and a `Scaffold` to `surface`, so any
// of these left unset would fall back to Material's default violet-grey neutrals
// and show up as a cold card on a warm page.
val ParchmentDimLight = Color(0xFFE5D8C8)
val ParchmentBrightLight = Color(0xFFFDF6ED)
val ParchmentContainerLowestLight = Color(0xFFFFFFFF)
val ParchmentContainerLowLight = Color(0xFFF8F1E7)
val ParchmentContainerLight = Color(0xFFF3EBE1)
val ParchmentContainerHighLight = Color(0xFFEEE6DB)
val ParchmentContainerHighestLight = Color(0xFFE8E0D4)

// Snackbars and other inverted surfaces
val InverseParchmentLight = Color(0xFF39342D)
val OnInverseParchmentLight = Color(0xFFF4EDE3)
val InverseTomeLeatherLight = Color(0xFFEDB595)

// --- Dark theme: primary, the leather lit by torchlight ---
val TomeLeatherDark = Color(0xFFEDB595)
val OnTomeLeatherDark = Color(0xFF562809)
val TomeLeatherContainerDark = Color(0xFF6E3C1D)
val OnTomeLeatherContainerDark = Color(0xFFFEDFCE)

// --- Dark theme: secondary, the slate ---
val DungeonSlateDark = Color(0xFFB8C6EA)
val OnDungeonSlateDark = Color(0xFF1F3960)
val DungeonSlateContainerDark = Color(0xFF314971)
val OnDungeonSlateContainerDark = Color(0xFFDCE5FE)

// --- Dark theme: tertiary, the arcane glow ---
val ArcaneGlowDark = Color(0xFF90D0E4)
val OnArcaneGlowDark = Color(0xFF003F4D)
val ArcaneGlowContainerDark = Color(0xFF014958)
val OnArcaneGlowContainerDark = Color(0xFFBFEDFC)

// --- Dark theme: error ---
val BloodErrorDark = Color(0xFFFFABAB)
val OnBloodErrorDark = Color(0xFF690019)
val BloodErrorContainerDark = Color(0xFF810022)
val OnBloodErrorContainerDark = Color(0xFFFEDAD9)

// --- Dark theme: neutrals, the near-black blue-violet behind the icon's frame ---
val CryptDark = Color(0xFF11101A)
val OnCryptDark = Color(0xFFE9E1D7)

// `surfaceVariant` stays warm while the surfaces around it are cold - sepia on
// blue-black is the icon's own "gold tooling on dark leather", and it keeps a
// NOTE card from being mistaken for the slate-blue DIALOGUE one.
val VellumVariantDark = Color(0xFF453A29)
val OnVellumVariantDark = Color(0xFFDFCCB2)
val OutlineDark = Color(0xFF9E8E7A)
val OutlineVariantDark = Color(0xFF4E4332)

val CryptDimDark = Color(0xFF11101A)
val CryptBrightDark = Color(0xFF35343D)
val CryptContainerLowestDark = Color(0xFF0B0916)
val CryptContainerLowDark = Color(0xFF171720)
val CryptContainerDark = Color(0xFF1B1B24)
val CryptContainerHighDark = Color(0xFF23232C)
val CryptContainerHighestDark = Color(0xFF2C2B35)

val InverseCryptDark = Color(0xFFE9E1D7)
val OnInverseCryptDark = Color(0xFF353027)
val InverseTomeLeatherDark = Color(0xFF875131)

/** Shared by both themes: the dim behind a modal is black in either. */
val ScrimBlack = Color(0xFF000000)
