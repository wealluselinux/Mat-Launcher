package com.matlauncher.app.ui

import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.matlauncher.app.data.HAlign
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.LockBackground
import com.matlauncher.app.data.LockClockStyle
import com.matlauncher.app.util.NotifItem
import com.matlauncher.app.util.NotificationStore
import com.matlauncher.app.util.SystemControls
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mat's lock screen: a solid Material You surface (no blurred wallpaper), huge stacked clock,
 * notifications, and quick toggles. It sits on top of the home screen and does not replace the
 * system lock.
 */
@Composable
fun LockScreen(
    locked: Boolean,
    settings: LauncherSettings,
    sys: SystemControls,
    onUnlock: () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val now by rememberNow()
    var showPad by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(locked) {
        if (locked) {
            showPad = false
            pin = ""
            error = false
            pending = null
            while (true) {
                sys.refresh()
                delay(2000)
            }
        }
    }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (settings.checkPin(pin)) {
                val p = pending
                onUnlock()
                p?.invoke()
            } else {
                error = true
                delay(550)
                error = false
            }
            pin = ""
        }
    }

    fun unlockThen(action: (() -> Unit)?) {
        if (settings.hasPin) {
            pending = action
            showPad = true
        } else {
            onUnlock()
            action?.invoke()
        }
    }

    val wall = settings.lockBackground == LockBackground.WALLPAPER
    val strong = when (settings.lockBackground) {
        LockBackground.GRADIENT -> scheme.onPrimaryContainer
        LockBackground.SOLID -> scheme.onSurface
        LockBackground.WALLPAPER -> Color.White
    }
    val accent = if (wall) Color.White.copy(alpha = 0.8f) else scheme.primary
    val subtle = if (wall) Color.White.copy(alpha = 0.8f) else scheme.onSurfaceVariant
    val centered = settings.lockAlign == HAlign.CENTER
    val is24 = DateFormat.is24HourFormat(ctx)
    val hh = SimpleDateFormat(if (is24) "HH" else "hh", Locale.getDefault()).format(Date(now))
    val mm = SimpleDateFormat("mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))

    Box(
        Modifier
            .fillMaxSize()
            .then(
                when (settings.lockBackground) {
                    LockBackground.GRADIENT -> Modifier.background(Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.surface)))
                    LockBackground.SOLID -> Modifier.background(scheme.surface)
                    LockBackground.WALLPAPER -> Modifier.background(Color.Black.copy(alpha = 0.5f))
                }
            )
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -160f) unlockThen(null)
                        else if (total > 160f && showPad) showPad = false
                    },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        // decorative cookie shapes, solid tonal colors
        if (!wall) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-60).dp)
                .size(300.dp)
                .clip(CookieShape)
                .background(scheme.secondaryContainer.copy(alpha = 0.55f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-110).dp, y = 90.dp)
                .size(280.dp)
                .clip(CookieShape)
                .background(scheme.tertiaryContainer.copy(alpha = 0.5f))
        )
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = strong, modifier = Modifier.size(18.dp))
                if (settings.lockShowDate) {
                    Spacer(Modifier.width(8.dp))
                    Text(date, color = strong, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(if (showPad) 4.dp else 12.dp))
            when (settings.lockClockStyle) {
                LockClockStyle.STACKED -> {
                    val size = if (showPad) 84.sp else 132.sp
                    Text(hh, color = strong, fontSize = size, lineHeight = size * 0.86f, fontWeight = FontWeight.Black)
                    Text(mm, color = accent, fontSize = size, lineHeight = size * 0.86f, fontWeight = FontWeight.Black)
                }
                LockClockStyle.INLINE -> {
                    val size = if (showPad) 64.sp else 92.sp
                    Text("$hh:$mm", color = strong, fontSize = size, fontWeight = FontWeight.Bold)
                }
                LockClockStyle.ANALOG -> {
                    val box = if (showPad) 150.dp else 240.dp
                    Box(
                        Modifier.size(box).clip(CookieShape).background(scheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnalogClock(now, Modifier.fillMaxSize().padding(20.dp))
                    }
                }
            }

            if (showPad) {
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            error -> scheme.error
                                            i < pin.length -> scheme.primary
                                            else -> scheme.onSurface.copy(alpha = 0.18f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (error) "Wrong PIN" else "Enter PIN",
                        color = subtle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<")
                    keys.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            row.forEach { k ->
                                PinKey(k) {
                                    if (k == "<") pin = pin.dropLast(1)
                                    else if (k.isNotEmpty() && pin.length < 4) pin += k
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Spacer(Modifier.height(12.dp))
                // ---- notifications
                if (!settings.lockShowNotifs) Spacer(Modifier.weight(1f))
                else Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!NotificationStore.hasAccess(ctx)) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Rounded.Notifications, null, tint = scheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text("Show notifications here", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Allow notification access to see them on the lock screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                FilledTonalButton(onClick = {
                                    unlockThen { openSystemScreen(ctx, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                                }) { Text("Allow") }
                            }
                        }
                    } else {
                        val items = NotificationStore.items
                        if (items.isEmpty()) {
                            Text("No new notifications", color = subtle, style = MaterialTheme.typography.bodyMedium)
                        }
                        val maxN = settings.lockMaxNotifs.coerceAtLeast(1)
                        items.take(maxN).forEach { n ->
                            NotificationCard(n) {
                                unlockThen {
                                    try { n.intent?.send() } catch (e: Exception) { /* canceled */ }
                                }
                            }
                        }
                        if (items.size > maxN) {
                            Text("+${items.size - maxN} more", color = subtle, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // ---- quick toggles
                if (settings.lockShowToggles) {
                    Spacer(Modifier.height(10.dp))
                    fun gated(action: () -> Unit) {
                        if (settings.hasPin) Toast.makeText(ctx, "Unlock to change this", Toast.LENGTH_SHORT).show()
                        else action()
                    }
                    val specs = settings.lockTiles.filter { it.onLock }.map { tileSpec(it, sys, onAi = {}, gate = { act -> gated(act) }) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        specs.forEach { QuickTile(it.icon, it.active, Modifier.weight(1f), it.onClick) }
                    }
                }

                // ---- unlock hint
                Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp, null, tint = strong,
                        modifier = Modifier.size(30.dp).clickable { unlockThen(null) },
                    )
                    Text("Swipe up to unlock", color = subtle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotifItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val icon = remember(n.key) { n.icon?.toBitmap(72, 72)?.asImageBitmap() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Image(icon, null, Modifier.size(36.dp).clip(CircleShape))
            }
            Column(Modifier.weight(1f)) {
                Text(n.appLabel, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
                if (n.title.isNotEmpty()) {
                    Text(n.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (n.text.isNotEmpty()) {
                    Text(
                        n.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (n.clearable) {
                Icon(
                    Icons.Rounded.Close, "Dismiss",
                    Modifier.size(32.dp).clip(CircleShape).clickable { NotificationStore.dismiss(n) }.padding(6.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(50),
        color = if (active) scheme.primary else scheme.surfaceContainerHighest,
        contentColor = if (active) scheme.onPrimary else scheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp)) }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    if (label.isEmpty()) {
        Box(Modifier.size(70.dp))
        return
    }
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "<") {
            Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = scheme.onSurface)
        } else {
            Text(label, color = scheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        }
    }
}
