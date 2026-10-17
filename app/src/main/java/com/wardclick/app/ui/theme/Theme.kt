package com.wardclick.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Цвета темы «Liquid Glass».
 * Фон прозрачный — под экранами рисуется GlassBackground (цветные пятна).
 * Карточки (surface / surfaceContainerHighest / *Container) — полупрозрачное стекло.
 * Диалоги, шторки и меню (surfaceContainerLow / surfaceContainer / surfaceContainerHigh)
 * почти непрозрачные, чтобы текст в них хорошо читался.
 */

private val Ink = Color(0xFF16141F)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0x405B4BDB),
    onPrimaryContainer = Color(0xFF2A1F7A),
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0x40FF8A65),
    onSecondaryContainer = Color(0xFF6B2410),
    tertiary = Accent,
    background = Color.Transparent,
    onBackground = Ink,
    surface = Color(0x8CFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0x66FFFFFF),
    onSurfaceVariant = Color(0xFF4A4659),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0x73FFFFFF),
    surfaceContainerLow = Color(0xF5F7F4FC),
    surfaceContainer = Color(0xF5F5F1FB),
    surfaceContainerHigh = Color(0xF7F3EFFA),
    surfaceContainerHighest = Color(0x8CFFFFFF),
    outline = Color(0xFF8D88A3),
    outlineVariant = Color(0x3316141F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA99BFF),
    onPrimary = Color(0xFF1E1458),
    primaryContainer = Color(0x40A99BFF),
    onPrimaryContainer = Color(0xFFE4DEFF),
    secondary = Color(0xFFFF9A80),
    onSecondary = Color(0xFF4A1405),
    secondaryContainer = Color(0x40FF8A65),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFFFF9A80),
    background = Color.Transparent,
    onBackground = Color(0xFFF2EFFA),
    surface = Color(0x66221F35),
    onSurface = Color(0xFFF2EFFA),
    surfaceVariant = Color(0x4D2A2740),
    onSurfaceVariant = Color(0xFFC9C4DA),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0x4D1A1829),
    surfaceContainerLow = Color(0xF51A1829),
    surfaceContainer = Color(0xF51E1C2E),
    surfaceContainerHigh = Color(0xF7232136),
    surfaceContainerHighest = Color(0x66221F35),
    outline = Color(0xFF8E89A3),
    outlineVariant = Color(0x33FFFFFF)
)

@Composable
fun WardClickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
