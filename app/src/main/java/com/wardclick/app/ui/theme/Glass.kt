package com.wardclick.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/*
 * Тема «Liquid Glass» (жидкое стекло).
 *
 *  - GlassBackground — мягкий «авроровый» фон из размытых цветных пятен, на котором видно стекло;
 *  - Modifier.liquidGlass — стеклянная поверхность: размытие того, что под ней (библиотека Haze,
 *    на Android 12+ настоящее размытие, на старых — полупрозрачная подложка), лёгкий тон,
 *    блик сверху и светлая кромка по краю, как у стекла.
 *
 * Размывать можно только то, что помечено hazeSource (контент экранов). Плавающие элементы
 * поверх контента (нижняя панель, кнопка «Заказ») используют blur = true,
 * карточки внутри экранов — blur = false (полупрозрачное стекло без размытия).
 */

/** Состояние Haze: какой контент размывать под стеклянными панелями. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Высота плавающей нижней панели — чтобы последние элементы списков не прятались под ней. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

object GlassDefaults {
    val CornerLarge = RoundedCornerShape(28.dp)
    val CornerMedium = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(50)
}

@Composable
private fun glassDark(): Boolean = isSystemInDarkTheme()

/** Цвет «стекла» без размытия (для карточек и плашек внутри экранов). */
@Composable
fun glassTint(): Color =
    if (glassDark()) Color(0xFF221F35).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)

/** Подсветка выбранного элемента на стекле. */
@Composable
fun glassSelected(): Color =
    if (glassDark()) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.72f)

/**
 * Стеклянная поверхность.
 * @param blur размывать то, что под поверхностью (только для элементов поверх контента экранов).
 * @param elevation мягкая тень под стеклом.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = GlassDefaults.CornerLarge,
    blur: Boolean = false,
    elevation: Dp = 0.dp
): Modifier {
    val dark = glassDark()
    val hazeState = LocalHazeState.current
    val tint = if (dark) Color(0xFF1E1B30).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.38f)
    val base = if (dark) Color(0xFF0F0D1A) else Color(0xFFF4F0FA)
    val shine = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) 0.10f else 0.38f),
        0.45f to Color.White.copy(alpha = 0f)
    )
    val rim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (dark) 0.38f else 0.95f),
            Color.White.copy(alpha = if (dark) 0.06f else 0.25f),
            Color.White.copy(alpha = if (dark) 0.22f else 0.70f)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    var m = this
    if (elevation > 0.dp) {
        m = m.shadow(
            elevation, shape,
            ambientColor = Color(0xFF2A1F5C).copy(alpha = 0.25f),
            spotColor = Color(0xFF2A1F5C).copy(alpha = 0.30f)
        )
    }
    m = m.clip(shape)
    m = if (blur && hazeState != null) {
        m.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = base,
                tint = HazeTint(tint),
                blurRadius = 26.dp,
                noiseFactor = 0.05f,
                fallbackTint = HazeTint(base.copy(alpha = 0.92f))
            )
        )
    } else {
        m.background(glassTint())
    }
    return m.background(shine).border(1.dp, rim, shape)
}

/** Фон приложения: мягкие цветные пятна, через которые «светится» стекло. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) Color(0xFF0D0B16) else Color(0xFFF4F0FA)
    // (цвет, центр по X, центр по Y, радиус — в долях ширины экрана)
    val blobs = if (dark) listOf(
        Blob(Color(0xFF5B3FD0), 0.10f, 0.08f, 0.85f, 0.55f),
        Blob(Color(0xFFA1347A), 1.00f, 0.38f, 0.75f, 0.35f),
        Blob(Color(0xFF1F5FBF), 0.05f, 0.78f, 0.85f, 0.40f),
        Blob(Color(0xFF127A6E), 0.95f, 1.00f, 0.70f, 0.30f)
    ) else listOf(
        Blob(Color(0xFFB9A8FF), 0.10f, 0.06f, 0.90f, 0.70f),
        Blob(Color(0xFFFFC3D6), 1.00f, 0.36f, 0.80f, 0.65f),
        Blob(Color(0xFFA8D8FF), 0.00f, 0.76f, 0.90f, 0.65f),
        Blob(Color(0xFFC9F2E3), 0.95f, 1.00f, 0.75f, 0.70f)
    )
    Canvas(modifier = modifier) {
        drawRect(base)
        blobs.forEach { b ->
            val center = Offset(size.width * b.x, size.height * b.y)
            val radius = size.width * b.r
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(b.color.copy(alpha = b.alpha), b.color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

private data class Blob(val color: Color, val x: Float, val y: Float, val r: Float, val alpha: Float)
