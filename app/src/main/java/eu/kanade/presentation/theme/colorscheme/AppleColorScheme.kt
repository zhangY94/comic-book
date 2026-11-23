package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Default theme of this fork, following the iOS system palette:
 * grouped page background, white cards, single blue tint and gray secondary labels.
 *
 * Key colors:
 * Tint #007AFF / #0A84FF
 * Page #F2F2F7 / #000000
 * Card #FFFFFF / #1C1C1E
 */
internal object AppleColorScheme : BaseColorScheme() {

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5F1FF),
        onPrimaryContainer = Color(0xFF00325C),
        inversePrimary = Color(0xFF0A84FF),
        secondary = Color(0xFF007AFF), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFFEDEDF2), // Navigation bar selector pill, search field
        onSecondaryContainer = Color(0xFF007AFF), // Navigation bar selector icon
        tertiary = Color(0xFF34C759), // Downloaded badge
        onTertiary = Color(0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(0xFFE3F8E7),
        onTertiaryContainer = Color(0xFF0B4A1C),
        background = Color(0xFFF2F2F7),
        onBackground = Color(0xFF0A0A0B),
        surface = Color(0xFFF2F2F7),
        onSurface = Color(0xFF0A0A0B),
        surfaceVariant = Color(0xFFF2F2F7), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF8A8A8E),
        surfaceTint = Color(0xFF007AFF),
        inverseSurface = Color(0xFF1C1C1E),
        inverseOnSurface = Color(0xFFF2F2F7),
        error = Color(0xFFFF3B30),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE5E3),
        onErrorContainer = Color(0xFF8B0000),
        outline = Color(0xFFC6C6CB),
        outlineVariant = Color(0xFFE4E4E9),
        surfaceContainerLowest = Color(0xFFFFFFFF), // Grouped list card
        surfaceContainerLow = Color(0xFFFAFAFC),
        surfaceContainer = Color(0xFFF2F2F7),
        surfaceContainerHigh = Color(0xFFFFFFFF),
        surfaceContainerHighest = Color(0xFFF7F7FA),
    )

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF002F5F),
        onPrimaryContainer = Color(0xFFCFE4FF),
        inversePrimary = Color(0xFF007AFF),
        secondary = Color(0xFF0A84FF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF2C2C2E), // Navigation bar selector pill, search field
        onSecondaryContainer = Color(0xFF0A84FF),
        tertiary = Color(0xFF30D158),
        onTertiary = Color(0xFF00230B),
        tertiaryContainer = Color(0xFF10361C),
        onTertiaryContainer = Color(0xFFB6F2C2),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF161618),
        onSurfaceVariant = Color(0xFF8E8E93),
        surfaceTint = Color(0xFF0A84FF),
        inverseSurface = Color(0xFFF2F2F7),
        inverseOnSurface = Color(0xFF0A0A0B),
        error = Color(0xFFFF453A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF5A1210),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF3A3A3C),
        outlineVariant = Color(0xFF2C2C2E),
        surfaceContainerLowest = Color(0xFF1C1C1E), // Grouped list card
        surfaceContainerLow = Color(0xFF141416),
        surfaceContainer = Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF202022),
        surfaceContainerHighest = Color(0xFF26262A),
    )
}
