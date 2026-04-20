package com.thxios.storagetree.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0.3f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = if (state.isScrollInProgress) 0 else 1000),
        label = "scrollbar_alpha"
    )
    drawWithContent {
        drawContent()
        val totalItems = state.layoutInfo.totalItemsCount
        if (totalItems == 0) return@drawWithContent
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@drawWithContent
        val visibleCount = visibleItems.size
        if (visibleCount >= totalItems) return@drawWithContent  // all items visible, no scrollbar needed

        val firstIndex = state.firstVisibleItemIndex
        val scrollbarHeight = (visibleCount.toFloat() / totalItems) * size.height
        val scrollbarY = (firstIndex.toFloat() / totalItems) * size.height

        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarY),
            size = Size(width.toPx(), scrollbarHeight),
            alpha = alpha
        )
    }
}
