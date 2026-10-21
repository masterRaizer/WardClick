package com.wardclick.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow

/*
 * Тема «Liquid Glass» (жидкое стекло) на библиотеке Kyant0/AndroidLiquidGlass (Kyant Backdrop,
 * Apache 2.0, см. THIRD_PARTY.md) — той же, на которой сделано стекло в приложении Ward.
 *
 *  - GlassBackground — мягкий «авроровый» фон из размытых цветных пятен, на котором видно стекло;
 *  - Modifier.liquidGlass — стеклянная поверхность: под ней размытая копия того, что за стеклом,
 *    у края преломление как у линзы (Android 13+), сверху лёгкая плёнка и блик по контуру.
 *    Размытие работает с Android 12, на более старых плёнка плотнее, чтобы текст читался;
 *  - нижняя панель вкладок и кнопка «Заказ» — компоненты LiquidBottomTabs и LiquidButton
 *    из каталога библиотеки (пакет com.kyant.backdrop.catalog), у них капля и пружинки.
 *
 * Что преломлять, стекло берёт из двух слоёв, которые пишет NavGraph:
 *  - LocalScreenBackdrop — весь экран (фон + контент): для элементов поверх контента
 *    (нижняя панель, кнопка «Заказ»);
 *  - LocalBackgroundBackdrop — только фон: для карточек внутри экранов. Общий слой им брать
 *    нельзя — они сами в него записываются, а рисовать слой внутри его же записи запрещено.
 */

/** Слой всего экрана — для стекла поверх контента. */
val LocalScreenBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Слой фона без контента — для стеклянных карточек внутри экранов. */
val LocalBackgroundBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Высота плавающей нижней панели — чтобы последние элементы списков не прятались под ней. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

object GlassDefaults {
    val CornerLarge = RoundedCornerShape(28.dp)
    val CornerMedium = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(50)

    /** Размытие того, что под стеклом. */
    val Blur = 6.dp
    /** Ширина полосы преломления у края и сила преломления. */
    val RefractionHeight = 12.dp
    val RefractionAmount = 20.dp
}

@Composable
private fun glassDark(): Boolean = isSystemInDarkTheme()

/** Цвет «стекла» без слоя под ним (поверх карты и т. п.). */
@Composable
fun glassTint(): Color =
    if (glassDark()) Color(0xFF221F35).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)

/**
 * Плёнка поверх размытой копии. Без размытия (Android 11 и старше) под стеклом видно
 * всё резко, поэтому плёнка плотнее.
 */
fun glassFilm(dark: Boolean, dense: Boolean = false): Color {
    val alpha = when {
        !isRenderEffectSupported() -> if (dark) 0.78f else 0.80f
        dense -> if (dark) 0.62f else 0.64f
        else -> if (dark) 0.34f else 0.36f
    }
    return if (dark) Color(0xFF1E1B30).copy(alpha = alpha) else Color.White.copy(alpha = alpha)
}

private val GlassShadowColor = Color(0xFF2A1F5C).copy(alpha = 0.22f)

/**
 * Стеклянная поверхность.
 * @param backdrop что видно сквозь стекло: по умолчанию фон экрана (для карточек внутри
 *   контента). null — без слоя: полупрозрачная плёнка, например поверх карты.
 * @param elevation мягкая тень под стеклом (рисуется только снаружи формы, под
 *   полупрозрачной карточкой серого пятна нет).
 * @param dense плотное стекло — для поверхностей с текстом поверх фото.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = GlassDefaults.CornerLarge,
    backdrop: Backdrop? = LocalBackgroundBackdrop.current,
    elevation: Dp = 0.dp,
    dense: Boolean = false
): Modifier {
    val dark = glassDark()
    val shine = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) 0.08f else 0.30f),
        0.45f to Color.White.copy(alpha = 0f)
    )
    if (backdrop == null) {
        val rim = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (dark) 0.38f else 0.95f),
                Color.White.copy(alpha = if (dark) 0.06f else 0.25f),
                Color.White.copy(alpha = if (dark) 0.22f else 0.70f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
        return clip(shape).background(glassTint()).background(shine).border(1.dp, rim, shape)
    }
    val film = glassFilm(dark, dense)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            // светлая тема — «вибрация» цветов как у Apple; на тёмной насыщенность скромнее,
            // иначе цветные пятна фона разгораются кляксами
            if (dark) colorControls(brightness = 0.02f, contrast = 1.1f, saturation = 1.2f) else vibrancy()
            blur(GlassDefaults.Blur.toPx())
            lens(GlassDefaults.RefractionHeight.toPx(), GlassDefaults.RefractionAmount.toPx())
        },
        highlight = { if (dark) Highlight.Default.copy(alpha = 0.7f) else Highlight.Default },
        shadow = if (elevation > 0.dp) {
            { Shadow(radius = elevation, color = GlassShadowColor) }
        } else {
            null
        },
        onDrawSurface = {
            drawRect(film)
            drawRect(shine)
        }
    ).clip(shape)
}

/**
 * Стеклянная карточка с контентом (списки аренд, админка и т. п.).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.CornerMedium,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .liquidGlass(shape, elevation = 6.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
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
