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

private val DarkColorScheme = darkColorScheme(
    primary = ArcaneVioletDark,
    onPrimary = OnArcaneVioletDark,
    primaryContainer = ArcaneVioletContainerDark,
    onPrimaryContainer = OnArcaneVioletContainerDark,

    secondary = DungeonSlateDark,
    onSecondary = OnDungeonSlateDark,
    secondaryContainer = DungeonSlateContainerDark,
    onSecondaryContainer = OnDungeonSlateContainerDark,

    tertiary = TorchAmberDark,
    onTertiary = OnTorchAmberDark,
    tertiaryContainer = TorchAmberContainerDark,
    onTertiaryContainer = OnTorchAmberContainerDark,

    error = BloodErrorDark,
    onError = OnBloodErrorDark,
    errorContainer = BloodErrorContainerDark,
    onErrorContainer = OnBloodErrorContainerDark,

    background = CryptDark,
    onBackground = OnCryptDark,
    surface = CryptDark,
    onSurface = OnCryptDark,
    surfaceVariant = CryptVariantDark,
    onSurfaceVariant = OnCryptVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ArcaneVioletLight,
    onPrimary = OnArcaneVioletLight,
    primaryContainer = ArcaneVioletContainerLight,
    onPrimaryContainer = OnArcaneVioletContainerLight,

    secondary = DungeonSlateLight,
    onSecondary = OnDungeonSlateLight,
    secondaryContainer = DungeonSlateContainerLight,
    onSecondaryContainer = OnDungeonSlateContainerLight,

    tertiary = TorchAmberLight,
    onTertiary = OnTorchAmberLight,
    tertiaryContainer = TorchAmberContainerLight,
    onTertiaryContainer = OnTorchAmberContainerLight,

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
    outline = OutlineLight
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
