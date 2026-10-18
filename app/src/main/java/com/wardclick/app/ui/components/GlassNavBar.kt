package com.wardclick.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wardclick.app.ui.theme.GlassDefaults
import com.wardclick.app.ui.theme.glassSelected
import com.wardclick.app.ui.theme.liquidGlass

data class GlassNavItem(val icon: ImageVector, val label: String)

/**
 * Плавающая стеклянная панель вкладок («Liquid Glass»): под ней размывается контент экрана,
 * а подсветка выбранной вкладки перетекает к новой с лёгкой пружинкой.
 */
@Composable
fun GlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .liquidGlass(GlassDefaults.Pill, blur = true, elevation = 14.dp, dense = true)
                .padding(5.dp)
        ) {
            val count = items.size.coerceAtLeast(1)
            val itemWidth = maxWidth / count
            val indicatorX by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0),
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "glassIndicator"
            )
            // «капля» выбранной вкладки
            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(GlassDefaults.Pill)
                        .background(glassSelected())
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.35f),
                                1f to Color.White.copy(alpha = 0f)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.6f), GlassDefaults.Pill)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val color by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navColor"
                    )
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .clip(GlassDefaults.Pill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(2.dp))
                        CompositionLocalProvider(LocalContentColor provides color) {
                            AutoSizeText(
                                item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Стеклянная кнопка-«пилюля» поверх контента (например, «Заказ: 2»). */
@Composable
fun GlassPillButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .liquidGlass(GlassDefaults.Pill, blur = true, elevation = 10.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
