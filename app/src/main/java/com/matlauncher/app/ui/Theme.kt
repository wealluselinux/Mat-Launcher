package com.matlauncher.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.matlauncher.app.R

/** Soft shadow so white text stays readable on any wallpaper. */
val OnWallpaper = TextStyle(
    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 8f)
)

/**
 * Material You: dynamic color on Android 12+, otherwise a scheme generated from the
 * wallpaper's primary color ([seed]).
 */
@Composable
fun MatTheme(seed: Int?, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> fallbackScheme(seed?.let { Color(it) } ?: Color(0xFF6750A4), dark)
    }
    MaterialTheme(colorScheme = scheme, typography = Typography().withFont(GoogleSans), content = content)
}

private fun fallbackScheme(p: Color, dark: Boolean): ColorScheme {
    val white = Color.White
    val black = Color.Black
    return if (dark) {
        val primary = lerp(p, white, 0.45f)
        darkColorScheme(
            primary = primary,
            onPrimary = lerp(p, black, 0.6f),
            primaryContainer = lerp(p, black, 0.45f),
            onPrimaryContainer = lerp(p, white, 0.8f),
            secondaryContainer = lerp(p, Color(0xFF2B2930), 0.6f),
            onSecondaryContainer = lerp(p, white, 0.85f),
            tertiaryContainer = lerp(p, Color(0xFF3B2F40), 0.55f),
            onTertiaryContainer = lerp(p, white, 0.85f),
            surface = Color(0xFF141218),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
        )
    } else {
        darkToLight(p)
    }
}

private fun darkToLight(p: Color): ColorScheme = lightColorScheme(
    primary = lerp(p, Color.Black, 0.15f),
    onPrimary = Color.White,
    primaryContainer = lerp(p, Color.White, 0.8f),
    onPrimaryContainer = lerp(p, Color.Black, 0.6f),
    secondaryContainer = lerp(p, Color.White, 0.85f),
    onSecondaryContainer = lerp(p, Color.Black, 0.6f),
    tertiaryContainer = lerp(p, Color(0xFFFFD8E4), 0.6f),
    onTertiaryContainer = lerp(p, Color.Black, 0.6f),
    surface = Color(0xFFFEF7FF),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
)

/** Google Sans (SIL Open Font License), bundled in res/font. */
val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)
