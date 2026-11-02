package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.i18n.MovioraLocalization

/**
 * Extended semantic color tokens tailored for MOVIORA's media streaming UI
 * (hero scrims, spotlight glows, poster fallbacks, tactical war maps, and VIP badges).
 */
@Immutable
data class MovioraExtendedColors(
    val cinematicBlack: Color,
    val luxuryGold: Color,
    val luxuryGoldBright: Color,
    val onLuxuryGold: Color,
    val deepPurple: Color,
    val royalPurple: Color,
    val posterGradientMid: Color,
    val posterGradientBase: Color,
    val heroScrimMid: Color,
    val heroScrimDeep: Color,
    val vipHeaderStart: Color,
    val tacticalMapSurface: Color,
    val offlineBannerContainer: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusDanger: Color,
    val statusOnDangerContainer: Color
)

val LocalMovioraExtendedColors = staticCompositionLocalOf {
    MovioraExtendedColors(
        cinematicBlack = CinematicBlack,
        luxuryGold = LuxuryGold,
        luxuryGoldBright = LuxuryGoldBright,
        onLuxuryGold = OnLuxuryGold,
        deepPurple = DeepPurple,
        royalPurple = RoyalPurple,
        posterGradientMid = CinematicPosterMid,
        posterGradientBase = CinematicPosterBase,
        heroScrimMid = CinematicScrim,
        heroScrimDeep = CinematicHeroScrimDeep,
        vipHeaderStart = VipCrownBronze,
        tacticalMapSurface = MovioraTacticalMapBg,
        offlineBannerContainer = MovioraOfflineBannerBg,
        statusSuccess = MovioraSuccess,
        statusWarning = MovioraWarning,
        statusDanger = MovioraDanger,
        statusOnDangerContainer = MovioraOnDangerContainer
    )
}

/**
 * Primary Material 3 Dark ColorScheme for MOVIORA:
 * - Background: Cinematic Black (#09090D)
 * - Primary: Luxury Gold (#D9B56D)
 * - Secondary / Accent: Royal & Deep Purple (#7560A8 / #2B1F47)
 */
val MovioraDarkColorScheme: ColorScheme = darkColorScheme(
    primary = LuxuryGold,
    onPrimary = OnLuxuryGold,
    primaryContainer = LuxuryGoldDeep,
    onPrimaryContainer = MovioraWarmIvory,
    inversePrimary = MovioraLightGold,
    secondary = RoyalPurple,
    onSecondary = OnDeepPurple,
    secondaryContainer = DeepPurple,
    onSecondaryContainer = OnDeepPurple,
    tertiary = LuxuryGoldBright,
    onTertiary = OnLuxuryGold,
    tertiaryContainer = DeepPurpleElevated,
    onTertiaryContainer = LuxuryGoldBright,
    background = CinematicBlack,
    onBackground = MovioraWarmIvory,
    surface = CinematicSurface,
    onSurface = MovioraWarmIvory,
    surfaceVariant = CinematicSurfaceHigh,
    onSurfaceVariant = MovioraMutedSilver,
    surfaceTint = LuxuryGold,
    inverseSurface = MovioraWarmIvory,
    inverseOnSurface = CinematicBlack,
    error = MovioraDanger,
    onError = Color.White,
    errorContainer = MovioraDangerContainer,
    onErrorContainer = MovioraOnDangerContainer,
    outline = MovioraHairline,
    outlineVariant = MovioraHairlineSubtle,
    scrim = CinematicScrim,
    surfaceBright = CinematicSurfaceHighest,
    surfaceDim = CinematicObsidianLowest,
    surfaceContainerLowest = CinematicObsidianLowest,
    surfaceContainerLow = CinematicSurfaceLow,
    surfaceContainer = CinematicSurfaceContainer,
    surfaceContainerHigh = CinematicSurfaceHigh,
    surfaceContainerHighest = CinematicSurfaceHighest
)

/**
 * Warm Ivory & Antique Gold Light ColorScheme for daytime viewing.
 */
val MovioraLightColorScheme: ColorScheme = lightColorScheme(
    primary = MovioraLightGold,
    onPrimary = Color.White,
    primaryContainer = MovioraLightGoldContainer,
    onPrimaryContainer = MovioraOnLightGoldContainer,
    inversePrimary = LuxuryGold,
    secondary = MovioraLightPurple,
    onSecondary = Color.White,
    secondaryContainer = MovioraLightPurpleContainer,
    onSecondaryContainer = MovioraOnLightPurpleContainer,
    tertiary = RoyalPurple,
    onTertiary = Color.White,
    tertiaryContainer = MovioraLightPurpleContainer,
    onTertiaryContainer = MovioraOnLightPurpleContainer,
    background = MovioraLightBg,
    onBackground = MovioraLightText,
    surface = MovioraLightSurface,
    onSurface = MovioraLightText,
    surfaceVariant = MovioraLightSurfaceVariant,
    onSurfaceVariant = MovioraLightMuted,
    surfaceTint = MovioraLightGold,
    inverseSurface = CinematicSurface,
    inverseOnSurface = MovioraWarmIvory,
    error = MovioraDanger,
    onError = Color.White,
    errorContainer = MovioraDangerContainer,
    onErrorContainer = MovioraOnDangerContainer,
    outline = MovioraLightBorder,
    outlineVariant = MovioraLightBorderSubtle,
    scrim = CinematicScrim,
    surfaceBright = MovioraLightSurfaceLowest,
    surfaceDim = MovioraLightSurfaceVariant,
    surfaceContainerLowest = MovioraLightSurfaceLowest,
    surfaceContainerLow = MovioraLightSurfaceLow,
    surfaceContainer = MovioraLightSurfaceVariant,
    surfaceContainerHigh = MovioraLightSurfaceVariant,
    surfaceContainerHighest = MovioraLightSurfaceHighest
)

/**
 * VIP Exclusive Schemes unlocked for active MOVIORA VIP subscribers.
 */
private val VipImperialGoldScheme: ColorScheme = MovioraDarkColorScheme.copy(
    primary = VipImperialGold,
    background = VipGoldNoirBg,
    surface = VipGoldNoirSurface,
    surfaceVariant = VipGoldNoirElevated,
    surfaceContainer = VipGoldNoirSurface,
    surfaceContainerHigh = VipGoldNoirElevated,
    outline = VipGoldNoirOutline
)

private val VipRoyalVelvetScheme: ColorScheme = MovioraDarkColorScheme.copy(
    primary = LuxuryGoldBright,
    secondary = VipAmethystAccent,
    background = VipVelvetBg,
    surface = VipVelvetSurface,
    surfaceVariant = VipVelvetElevated,
    surfaceContainer = VipVelvetSurface,
    surfaceContainerHigh = VipVelvetElevated,
    outline = VipVelvetOutline
)

/**
 * Material 3 Shape Scale tailored for Android cinema & media interfaces:
 * - extraSmall: Metadata tags & quality badges
 * - small: Filter chips & compact action buttons
 * - medium: Movie/series poster cards & episode rows
 * - large: Featured rails & elevated panels
 * - extraLarge: Hero banners & bottom modal sheets
 */
val MovioraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val MaterialTheme.movioraColors: MovioraExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMovioraExtendedColors.current

@Composable
fun MovioraTheme(
    themeMode: String = "dark",
    languageCode: String = "ar",
    isVipActive: Boolean = false,
    content: @Composable () -> Unit
) {
    val isLightMode = themeMode == "light"
    val effectiveScheme = when (themeMode) {
        "light" -> MovioraLightColorScheme
        "vip_gold" -> if (isVipActive) VipImperialGoldScheme else MovioraDarkColorScheme
        "vip_velvet" -> if (isVipActive) VipRoyalVelvetScheme else MovioraDarkColorScheme
        else -> MovioraDarkColorScheme
    }

    val extendedColors = MovioraExtendedColors(
        cinematicBlack = CinematicBlack,
        luxuryGold = effectiveScheme.primary,
        luxuryGoldBright = LuxuryGoldBright,
        onLuxuryGold = OnLuxuryGold,
        deepPurple = DeepPurple,
        royalPurple = effectiveScheme.secondary,
        posterGradientMid = CinematicPosterMid,
        posterGradientBase = CinematicPosterBase,
        heroScrimMid = CinematicScrim,
        heroScrimDeep = CinematicHeroScrimDeep,
        vipHeaderStart = VipCrownBronze,
        tacticalMapSurface = MovioraTacticalMapBg,
        offlineBannerContainer = MovioraOfflineBannerBg,
        statusSuccess = MovioraSuccess,
        statusWarning = MovioraWarning,
        statusDanger = MovioraDanger,
        statusOnDangerContainer = MovioraOnDangerContainer
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLightMode
            insetsController.isAppearanceLightNavigationBars = isLightMode
        }
    }

    val dictionary = MovioraLocalization.dictionaryFor(languageCode)
    val layoutDirection = MovioraLocalization.layoutDirectionFor(languageCode)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalMovioraStrings provides dictionary,
        LocalAppLanguage provides languageCode,
        LocalMovioraExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = effectiveScheme,
            typography = movioraTypographyFor(languageCode),
            shapes = MovioraShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MovioraTheme(
        themeMode = if (darkTheme) "dark" else "light",
        content = content
    )
}
