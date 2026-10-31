package com.matlauncher.app.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.LauncherSettings

/** Shared state for dragging an app around the home screen in edit mode. */
class AppDragState {
    val bounds = HashMap<String, Rect>()
    var dragKey by mutableStateOf<String?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(Size.Zero)
    /** -1 near the left edge, 1 near the right edge, 0 otherwise. */
    var edge by mutableIntStateOf(0)

    fun reset() {
        dragKey = null
        edge = 0
    }
}

/** An app cell that can be picked up (long-press) and dropped somewhere else while editing. */
@Composable
fun DraggableApp(
    app: AppInfo,
    settings: LauncherSettings,
    actions: LauncherActions,
    drag: AppDragState,
    screenWidthPx: Float,
    onDrop: (key: String, pointer: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragging = drag.dragKey == app.key
    Box(
        modifier
            .onGloballyPositioned { drag.bounds[app.key] = it.boundsInRoot() }
            .alpha(if (dragging) 0.25f else 1f)
    ) {
        AppCell(app, settings, actions, Modifier)
        // Covers the cell so taps don't launch the app while editing.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(app.key) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            val b = drag.bounds[app.key] ?: return@detectDragGesturesAfterLongPress
                            drag.pointer = b.center
                            drag.size = b.size
                            drag.dragKey = app.key
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            drag.pointer += delta
                            val margin = 48f * density
                            drag.edge = when {
                                drag.pointer.x < margin -> -1
                                drag.pointer.x > screenWidthPx - margin -> 1
                                else -> 0
                            }
                        },
                        onDragEnd = {
                            val k = drag.dragKey
                            val p = drag.pointer
                            drag.reset()
                            if (k != null) onDrop(k, p)
                        },
                        onDragCancel = { drag.reset() },
                    )
                }
        )
    }
}
