package com.matlauncher.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matlauncher.app.data.LauncherSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A launcher-level lock screen. It sits on top of the home screen (it does not replace the
 * system lock), shows a big Material You clock, and optionally asks for a 4 digit PIN.
 */
@Composable
fun LockScreen(locked: Boolean, settings: LauncherSettings, onUnlock: () -> Unit) {
    val ctx = LocalContext.current
    val now by rememberNow()
    var showPad by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(locked) {
        if (locked) {
            showPad = false
            pin = ""
            error = false
        }
    }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (settings.checkPin(pin)) {
                onUnlock()
            } else {
                error = true
                delay(550)
                error = false
            }
            pin = ""
        }
    }

    val is24 = DateFormat.is24HourFormat(ctx)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.62f))))
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -160f) {
                            if (settings.hasPin) showPad = true else onUnlock()
                        } else if (total > 160f && showPad) {
                            showPad = false
                        }
                    },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (showPad) 24.dp else 96.dp))
            Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                time,
                color = Color.White,
                fontSize = if (showPad) 64.sp else 104.sp,
                fontWeight = FontWeight.Light,
                style = MaterialTheme.typography.displayLarge.merge(OnWallpaper),
            )
            Text(date, color = Color.White, style = MaterialTheme.typography.titleLarge.merge(OnWallpaper))

            if (!showPad) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.KeyboardArrowUp, null, tint = Color.White,
                    modifier = Modifier.size(32.dp).clickable { if (settings.hasPin) showPad = true else onUnlock() },
                )
                Text(
                    "Swipe up to unlock",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.merge(OnWallpaper),
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(24.dp))
                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        error -> MaterialTheme.colorScheme.error
                                        i < pin.length -> Color.White
                                        else -> Color.White.copy(alpha = 0.3f)
                                    }
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (error) "Wrong PIN" else "Enter PIN",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<")
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 7.dp)) {
                        row.forEach { k ->
                            PinKey(k) {
                                if (k == "<") pin = pin.dropLast(1)
                                else if (k.isNotEmpty() && pin.length < 4) pin += k
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    if (label.isEmpty()) {
        Box(Modifier.size(72.dp))
        return
    }
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "<") {
            Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = Color.White)
        } else {
            Text(label, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Normal)
        }
    }
}
