package com.matlauncher.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.matlauncher.app.data.AppInfo
import kotlinx.coroutines.delay

/** Where the last tapped icon was on screen, so the open animation can grow out of it. */
object LaunchOrigin {
    var rect: Rect? = null
    fun consume(): Rect? = rect.also { rect = null }
}

class LaunchRequest(val app: AppInfo, val origin: Rect?)

/**
 * Plays a short "grow from the icon" animation, then starts the app (the system adds its own
 * scale-up transition on top), then fades away.
 */
@Composable
fun LaunchAnimation(req: LaunchRequest, onStart: (AppInfo, Rect?) -> Unit, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val progress = remember(req) { Animatable(0f) }
    val fade = remember(req) { Animatable(1f) }

    LaunchedEffect(req) {
        progress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        onStart(req.app, req.origin)
        delay(120)
        fade.animateTo(0f, tween(260))
        onDone()
    }

    Canvas(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }
    ) {
        val full = Rect(Offset.Zero, size)
        val start = req.origin ?: Rect(
            Offset(size.width / 2f - 40f, size.height / 2f - 40f),
            Size(80f, 80f),
        )
        val p = progress.value
        val l = start.left + (full.left - start.left) * p
        val t = start.top + (full.top - start.top) * p
        val r = start.right + (full.right - start.right) * p
        val b = start.bottom + (full.bottom - start.bottom) * p
        val radius = (1f - p) * 48f
        drawRoundRect(
            color = lerp(scheme.primaryContainer, scheme.surface, p).copy(alpha = fade.value),
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
}
