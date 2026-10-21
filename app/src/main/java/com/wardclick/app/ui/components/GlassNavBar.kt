package com.wardclick.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidButton
import com.wardclick.app.ui.theme.LocalScreenBackdrop
import com.wardclick.app.ui.theme.glassFilm

data class GlassNavItem(val icon: ImageVector, val label: String)

/** Жёсткость хода капли: мягче, чем в каталоге, — у нас до шести вкладок и ход длинный. */
private const val TabTravelStiffness = 500f

/**
 * Плавающая панель вкладок «жидкое стекло» — LiquidBottomTabs из каталога Kyant Backdrop:
 * под капсулой размывается и преломляется контент экрана, выбранную вкладку отмечает
 * капля-линза, которую можно тянуть пальцем по панели.
 */
@Composable
fun GlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalScreenBackdrop.current ?: emptyBackdrop()
    val scheme = MaterialTheme.colorScheme
    // Лямбда одна на всё время: компонент запоминает по ней своё состояние
    val selectedState = rememberUpdatedState(selectedIndex)
    val selectedTabIndex = remember { { selectedState.value.coerceAtLeast(0) } }
    // Компонент сообщает и о смене вкладки снаружи — переходить туда повторно не нужно.
    // Обработчик он запоминает при первом показе, поэтому берём всегда актуальный onSelect
    val onSelectState = rememberUpdatedState(onSelect)
    val select: (Int) -> Unit = remember {
        { index: Int -> if (index != selectedState.value) onSelectState.value(index) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 6.dp)
    ) {
        val count = items.size.coerceAtLeast(1)
        // одинаковый размер подписей у всех вкладок — подбирается по самой длинной,
        // иначе «Избранное» сжималась сильнее соседних подписей
        val itemWidth = (maxWidth - 8.dp) / count
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val baseStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        val labelStyle = remember(items, itemWidth, baseStyle, density) {
            val available = with(density) { (itemWidth - 6.dp).toPx() }
            var scale = 1f
            while (scale > 0.6f && items.any {
                    measurer.measure(it.label, baseStyle.copy(fontSize = baseStyle.fontSize * scale), maxLines = 1, softWrap = false)
                        .size.width > available
                }
            ) {
                scale -= 0.05f
            }
            baseStyle.copy(fontSize = baseStyle.fontSize * scale)
        }
        // число вкладок меняется при входе администратора — тогда панель собирается заново
        key(count) {
            LiquidBottomTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = select,
                backdrop = backdrop,
                tabsCount = count,
                accentColor = scheme.primary,
                // подписи лежат поверх фото в ленте — плёнка плотнее, чем в каталоге
                containerColor = glassFilm(isSystemInDarkTheme(), dense = true),
                stiffness = TabTravelStiffness
            ) {
                items.forEachIndexed { index, item ->
                    LiquidBottomTab(
                        onClick = { select(index) },
                        modifier = Modifier.semantics { selected = index == selectedIndex }
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            item.label,
                            style = labelStyle,
                            color = scheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/** Стеклянная кнопка-«пилюля» поверх контента (например, «Заказ: 2») — LiquidButton из каталога. */
@Composable
fun GlassPillButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidButton(
        onClick = onClick,
        backdrop = LocalScreenBackdrop.current ?: emptyBackdrop(),
        modifier = modifier,
        surfaceColor = glassFilm(isSystemInDarkTheme(), dense = true)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
