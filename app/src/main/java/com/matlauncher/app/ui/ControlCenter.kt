package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.CcStyle
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.util.SystemControls
import kotlinx.coroutines.delay

@Composable
fun ControlCenter(sys: SystemControls, settings: LauncherSettings, actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    val now by rememberNow()

    LaunchedEffect(Unit) {
        while (true) {
            sys.refresh()
            delay(1500)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // scrim: tap outside to close
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    actions.closeOverlay()
                }
        )

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
            color = scheme.surface.copy(alpha = 0.97f),
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // header
                if (settings.ccShowHeader) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(now)),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date(now)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                    ) {
                        Text(
                            (if (sys.charging) "⚡ " else "") + sys.battery + "%",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    SmallAction(Icons.Rounded.Notifications, "Notifications") { sys.expandNotifications() }
                }

                // tiles
                val specs = settings.ccTiles.map { tileSpec(it, sys, onAi = { actions.openAi() }, gate = { act -> act() }) }
                if (settings.ccStyle == CcStyle.LARGE) {
                    specs.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { TileView(it, Modifier.weight(1f)) }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    specs.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { CompactTile(it, Modifier.weight(1f)) }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // sliders
                if (settings.ccShowBrightness) {
                    var bright by remember { mutableFloatStateOf(sys.brightness) }
                    LaunchedEffect(sys.brightness) { bright = sys.brightness }
                    SliderCard(Icons.Rounded.Brightness6, bright, onChange = { bright = it }, onFinish = { sys.setBrightness(bright) })
                }

                if (settings.ccShowVolume) {
                    var vol by remember { mutableFloatStateOf(sys.volume) }
                    LaunchedEffect(sys.volume) { vol = sys.volume }
                    SliderCard(Icons.AutoMirrored.Rounded.VolumeUp, vol, onChange = { vol = it; sys.changeVolume(it) }, onFinish = {})
                }

                // shortcuts
                if (settings.ccShowShortcuts) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Shortcut(Icons.Rounded.Wallpaper, "Wallpaper", Modifier.weight(1f)) { actions.showWallpaperGallery() }
                        Shortcut(Icons.Rounded.Lock, "Lock", Modifier.weight(1f)) { actions.lockNow() }
                        Shortcut(Icons.Rounded.Settings, "Mat settings", Modifier.weight(1f)) { actions.openSettings() }
                    }
                }

                // drag handle: swipe up or tap to close
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var total = 0f
                            detectVerticalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = { if (total < -40f) actions.closeOverlay() },
                                onDragCancel = { total = 0f },
                                onVerticalDrag = { _, dy -> total += dy },
                            )
                        }
                        .clickable { actions.closeOverlay() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun TileView(t: TileSpec, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = t.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = if (t.active) scheme.primary else scheme.surfaceContainerHighest,
        contentColor = if (t.active) scheme.onPrimary else scheme.onSurface,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(t.icon, null, Modifier.size(26.dp))
            Column {
                Text(t.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CompactTile(t: TileSpec, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = t.onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(50),
        color = if (t.active) scheme.primary else scheme.surfaceContainerHighest,
        contentColor = if (t.active) scheme.onPrimary else scheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(t.icon, t.label, Modifier.size(26.dp)) }
    }
}

@Composable
private fun SliderCard(icon: ImageVector, value: Float, onChange: (Float) -> Unit, onFinish: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = scheme.surfaceContainerHighest,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = scheme.primary)
            Slider(
                value = value.coerceIn(0f, 1f),
                onValueChange = onChange,
                onValueChangeFinished = onFinish,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Column(
            Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Icon(icon, desc, Modifier.padding(8.dp).size(22.dp))
    }
}
