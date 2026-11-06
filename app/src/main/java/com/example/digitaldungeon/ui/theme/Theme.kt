package com.example.digitaldungeon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Both schemes assign every role Material 3 exposes, including the ones no
 * screen names directly. An unassigned role is not "unused": it keeps the
 * baseline Material value, which is violet-grey, and components reach for those
 * roles on their own - a filled `Card` resolves to `surfaceContainerHighest`,
 * a `Snackbar` to `inverseSurface`, a dialog scrim to `scrim`. Leaving them out
 * is how a palette ends up half its own and half Material's default.
 *
 * See the notes at the top of Color.kt for where the colors come from and why
 * the tones differ from the stock Material ladder in two places.
 */
private val DarkColorScheme = darkColorScheme(
    primary = TomeLeatherDark,
    onPrimary = OnTomeLeatherDark,
    primaryContainer = TomeLeatherContainerDark,
    onPrimaryContainer = OnTomeLeatherContainerDark,
    inversePrimary = InverseTomeLeatherDark,

    secondary = DungeonSlateDark,
    onSecondary = OnDungeonSlateDark,
    secondaryContainer = DungeonSlateContainerDark,
    onSecondaryContainer = OnDungeonSlateContainerDark,

    tertiary = ArcaneGlowDark,
    onTertiary = OnArcaneGlowDark,
    tertiaryContainer = ArcaneGlowContainerDark,
    onTertiaryContainer = OnArcaneGlowContainerDark,

    error = BloodErrorDark,
    onError = OnBloodErrorDark,
    errorContainer = BloodErrorContainerDark,
    onErrorContainer = OnBloodErrorContainerDark,

    background = CryptDark,
    onBackground = OnCryptDark,
    surface = CryptDark,
    onSurface = OnCryptDark,
    surfaceVariant = VellumVariantDark,
    onSurfaceVariant = OnVellumVariantDark,
    surfaceTint = TomeLeatherDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    surfaceDim = CryptDimDark,
    surfaceBright = CryptBrightDark,
    surfaceContainerLowest = CryptContainerLowestDark,
    surfaceContainerLow = CryptContainerLowDark,
    surfaceContainer = CryptContainerDark,
    surfaceContainerHigh = CryptContainerHighDark,
    surfaceContainerHighest = CryptContainerHighestDark,

    inverseSurface = InverseCryptDark,
    inverseOnSurface = OnInverseCryptDark,
    scrim = ScrimBlack
)

private val LightColorScheme = lightColorScheme(
    primary = TomeLeatherLight,
    onPrimary = OnTomeLeatherLight,
    primaryContainer = TomeLeatherContainerLight,
    onPrimaryContainer = OnTomeLeatherContainerLight,
    inversePrimary = InverseTomeLeatherLight,

    secondary = DungeonSlateLight,
    onSecondary = OnDungeonSlateLight,
    secondaryContainer = DungeonSlateContainerLight,
    onSecondaryContainer = OnDungeonSlateContainerLight,

    tertiary = ArcaneGlowLight,
    onTertiary = OnArcaneGlowLight,
    tertiaryContainer = ArcaneGlowContainerLight,
    onTertiaryContainer = OnArcaneGlowContainerLight,

    error = BloodErrorLight,
    onError = OnBloodErrorLight,
    errorContainer = BloodErrorContainerLight,
    onErrorContainer = OnBloodErrorContainerLight,

    background = ParchmentLight,
    onBackground = OnParchmentLight,
    surface = ParchmentLight,
    onSurface = OnParchmentLight,
    surfaceVariant = ParchmentVariantLight,
    onSurfaceVariant = OnParchmentVariantLight,
    surfaceTint = TomeLeatherLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    surfaceDim = ParchmentDimLight,
    surfaceBright = ParchmentBrightLight,
    surfaceContainerLowest = ParchmentContainerLowestLight,
    surfaceContainerLow = ParchmentContainerLowLight,
    surfaceContainer = ParchmentContainerLight,
    surfaceContainerHigh = ParchmentContainerHighLight,
    surfaceContainerHighest = ParchmentContainerHighestLight,

    inverseSurface = InverseParchmentLight,
    inverseOnSurface = OnInverseParchmentLight,
    scrim = ScrimBlack
)

@Composable
fun DigitalDungeonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: dynamic color would replace the app's own palette on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
