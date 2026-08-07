package cn.a10miaomiao.bilimiao.compose.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.coerceAtLeast
import com.a10miaomiao.bilimiao.store.WindowStore

@Composable
fun WindowStore.Insets.addPaddingValues(
    addLeft: Dp = 0.dp,
    addRight: Dp = 0.dp,
    addTop: Dp = 0.dp,
    addBottom: Dp = 0.dp,
): PaddingValues {
    return remember(this, addLeft, addRight, addTop, addBottom) {
        PaddingValues.Absolute(
            left = max(leftDp.dp + addLeft, 0.dp),
            right = max(rightDp.dp + addRight, 0.dp),
            top = max(topDp.dp + addTop, 0.dp),
            bottom = max(bottomDp.dp + addBottom, 0.dp),
        )
    }
}

@Composable
fun WindowStore.Insets.toPaddingValues(
    left: Dp? = null,
    right: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
): PaddingValues {
    return remember(this, left, right, top, bottom) {
        PaddingValues.Absolute(
            left = (left ?: leftDp.dp).coerceAtLeast(0.dp),
            right = (right ?: rightDp.dp).coerceAtLeast(0.dp),
            top = (top ?: topDp.dp).coerceAtLeast(0.dp),
            bottom = (bottom ?: bottomDp.dp).coerceAtLeast(0.dp),
        )
    }
}

fun WindowStore.Insets.toWindowInsets(
    addLeft: Dp = 0.dp,
    addRight: Dp = 0.dp,
    addTop: Dp = 0.dp,
    addBottom: Dp = 0.dp,
): WindowInsets {
    return object : WindowInsets {
        override fun getBottom(density: Density): Int {
            return maxOf(bottom + density.run { addBottom.roundToPx() }, 0)
        }

        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
            return maxOf(left + density.run { addLeft.roundToPx() }, 0)
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
            return maxOf(right + density.run { addRight.roundToPx() }, 0)
        }

        override fun getTop(density: Density): Int {
            return maxOf(top + density.run { addTop.roundToPx() }, 0)
        }
    }
}