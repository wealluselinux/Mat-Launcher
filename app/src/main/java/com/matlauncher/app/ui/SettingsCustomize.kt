package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.matlauncher.app.data.EdgeZone
import com.matlauncher.app.util.MatAccessibilityService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.matlauncher.app.data.CcStyle
import com.matlauncher.app.data.DateStyle
import com.matlauncher.app.data.HAlign
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.LockBackground
import com.matlauncher.app.data.LockClockStyle
import com.matlauncher.app.data.TextScale
import com.matlauncher.app.data.TileId
import com.matlauncher.app.util.UpdateState
import com.matlauncher.app.util.Updater
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------- updates

/** A gradient per release. 1.3 green, 1.4 amber, 1.5 sky blue, 1.6 purple-black; later ones are seeded random. */
private fun bannerColors(version: String): Pair<Color, Color> {
    val parts = version.split('.').mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 1 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    if (major == 1) when (minor) {
        2 -> return Color(0xFFE05A8C) to Color(0xFF7B3FA0)
        3 -> return Color(0xFF2E9E57) to Color(0xFF0B5A34)
        4 -> return Color(0xFFF2B01E) to Color(0xFFC2661A)
        5 -> return Color(0xFF5CC8FF) to Color(0xFF2478D6)
        6 -> return Color(0xFF6B3FD1) to Color(0xFF120A24)
    }
    val rnd = java.util.Random((major * 10000L + minor * 100L + patch) * 7919L + 13L)
    val hue = rnd.nextFloat() * 360f
    val hue2 = (hue + 25f + rnd.nextFloat() * 50f) % 360f
    return Color.hsv(hue, 0.65f, 0.9f) to Color.hsv(hue2, 0.8f, 0.4f)
}

@Composable
internal fun UpdateBanner(version: String) {
    val (a, b) = bannerColors(version)
    val primary = MaterialTheme.colorScheme.primary
    // a little of the wallpaper palette so it still feels like part of the theme
    val c1 = lerp(a, primary, 0.15f)
    val c2 = lerp(b, primary, 0.10f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(170.dp)
                .align(Alignment.TopEnd)
                .padding(top = 0.dp)
                .clip(RoundedCornerShape(85.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Mat Launcher", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Text(version, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun UpdateCard(updater: Updater, settings: LauncherSettings, weatherInstalled: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    @Suppress("UNUSED_VARIABLE") val recompose = tick

    val launcherV = updater.installedLauncherVersion(ctx)
    val weatherV = updater.installedWeatherVersion(ctx)
    val st = updater.state

    Card {
        Text("Installed", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        Text(
            "Mat Launcher $launcherV" + if (weatherInstalled && weatherV != null) "  ·  Mat Weather $weatherV" else "",
            style = MaterialTheme.typography.titleMedium,
        )

        when (st) {
            is UpdateState.Idle, is UpdateState.UpToDate, is UpdateState.Error -> {
                val msg = when (st) {
                    is UpdateState.UpToDate -> "You're up to date."
                    is UpdateState.Error -> st.message
                    else -> "Checks the latest release on GitHub."
                }
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (st is UpdateState.Error) scheme.error else scheme.onSurfaceVariant,
                )
                Button(onClick = { scope.launch { updater.check(ctx) } }) { Text("Check for updates") }
            }
            is UpdateState.Checking -> {
                Text("Checking for updates…", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            is UpdateState.Available -> {
                val i = st.info
                Text("Version ${i.version} is available", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                Text(
                    buildString {
                        if (i.needLauncher) append("Mat Launcher $launcherV → ${i.version}\n")
                        if (i.needWeather) append("Mat Weather $weatherV → ${i.version}")
                    }.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (i.notes.isNotBlank()) {
                    Text(
                        i.notes.take(600) + if (i.notes.length > 600) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { scope.launch { updater.download(ctx) } }) { Text("Download update") }
            }
            is UpdateState.Downloading -> {
                Text("Downloading ${st.label}…", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Ready -> {
                val weatherPending = st.weatherFile != null && !updater.weatherDone(ctx, st.info)
                Text("Ready to install ${st.info.version}", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                if (st.weatherFile != null) {
                    if (weatherPending) {
                        Button(onClick = { updater.install(ctx, st.weatherFile) }) { Text("1. Install Mat Weather update") }
                    } else {
                        Text("Mat Weather is up to date.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (st.launcherFile != null) {
                    val label = (if (st.weatherFile != null) "2. " else "") + "Install Mat Launcher ${st.info.version}"
                    if (weatherPending) {
                        FilledTonalButton(onClick = { updater.install(ctx, st.launcherFile) }) { Text(label) }
                    } else {
                        Button(onClick = { updater.install(ctx, st.launcherFile) }) { Text(label) }
                    }
                }
                Text(
                    "Android asks you to confirm each install. Mat Launcher closes and reopens after it updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        SwitchRow("Check automatically", settings.autoUpdateCheck) { settings.autoUpdateCheck = it }
    }
}

// ---------------------------------------------------------------------------- top bar

@Composable
internal fun TopBarSettings(settings: LauncherSettings) {
    Card {
        SwitchRow("Show header (date & weather)", settings.topBarShow) { settings.topBarShow = it }
        SwitchRow("Keep on every home screen", settings.topBarAllPages) { settings.topBarAllPages = it }
        SwitchRow("Show date", settings.topBarDate) { settings.topBarDate = it }
        SwitchRow("Show weather", settings.topBarWeather) { settings.topBarWeather = it }
        Label("Date format")
        Chips(DateStyle.values().toList(), settings.topBarDateStyle, { it.label }) { settings.topBarDateStyle = it }
        Label("Alignment")
        Chips(HAlign.values().toList(), settings.topBarAlign, { it.label }) { settings.topBarAlign = it }
        Label("Text size")
        Chips(TextScale.values().toList(), settings.topBarSize, { it.label }) { settings.topBarSize = it }
        SwitchRow("Hide the status bar", settings.hideStatusBar) { settings.hideStatusBar = it }
        Text(
            "With the status bar hidden, swipe down from the very top to peek at it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------- lock screen

@Composable
internal fun LockLookSettings(settings: LauncherSettings) {
    Card {
        Label("Background")
        Chips(LockBackground.values().toList(), settings.lockBackground, { it.label }) { settings.lockBackground = it }
        Label("Clock style")
        Chips(LockClockStyle.values().toList(), settings.lockClockStyle, { it.label }) { settings.lockClockStyle = it }
        Label("Alignment")
        Chips(HAlign.values().toList(), settings.lockAlign, { it.label }) { settings.lockAlign = it }
        SwitchRow("Show date", settings.lockShowDate) { settings.lockShowDate = it }
        SwitchRow("Show notifications", settings.lockShowNotifs) { settings.lockShowNotifs = it }
        if (settings.lockShowNotifs) {
            Label("Notifications shown")
            Chips(listOf(3, 5, 8), settings.lockMaxNotifs, { "$it" }) { settings.lockMaxNotifs = it }
        }
        SwitchRow("Show quick toggles", settings.lockShowToggles) { settings.lockShowToggles = it }
        if (settings.lockShowToggles) {
            Label("Toggles (in order)")
            TileListEditor(
                all = TileId.values().filter { it.onLock },
                selected = settings.lockTiles,
                max = 5,
                onChange = { settings.setLockTiles(it) },
            )
        }
    }
}

// ---------------------------------------------------------------------------- control center

@Composable
internal fun ControlCenterSettings(settings: LauncherSettings) {
    Card {
        Label("Tile style")
        Chips(CcStyle.values().toList(), settings.ccStyle, { it.label }) { settings.ccStyle = it }
        SwitchRow("Show clock & battery header", settings.ccShowHeader) { settings.ccShowHeader = it }
        SwitchRow("Brightness slider", settings.ccShowBrightness) { settings.ccShowBrightness = it }
        SwitchRow("Volume slider", settings.ccShowVolume) { settings.ccShowVolume = it }
        SwitchRow("Wallpaper / Lock / Settings shortcuts", settings.ccShowShortcuts) { settings.ccShowShortcuts = it }
        Label("Tiles (in order)")
        TileListEditor(
            all = TileId.values().toList(),
            selected = settings.ccTiles,
            max = 12,
            onChange = { settings.setCcTiles(it) },
        )
    }
}

/** Reorderable list of the tiles that are on, plus chips to add the ones that are off. */
@Composable
internal fun TileListEditor(all: List<TileId>, selected: List<TileId>, max: Int, onChange: (List<TileId>) -> Unit) {
    val shown = selected.filter { it in all }
    shown.forEachIndexed { i, t ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(enabled = i > 0, onClick = {
                val l = shown.toMutableList(); l[i] = l[i - 1].also { l[i - 1] = l[i] }; onChange(l)
            }) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
            IconButton(enabled = i < shown.lastIndex, onClick = {
                val l = shown.toMutableList(); l[i] = l[i + 1].also { l[i + 1] = l[i] }; onChange(l)
            }) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
            IconButton(onClick = { onChange(shown - t) }) { Icon(Icons.Rounded.Close, "Remove") }
        }
    }
    val rest = all.filter { it !in shown }
    if (rest.isNotEmpty() && shown.size < max) {
        Label("Add a tile")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rest.forEach { t ->
                AssistChip(
                    onClick = { onChange(shown + t) },
                    label = { Text(t.label) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null) },
                )
            }
        }
    }
}


// ---------------------------------------------------------------------------- over other apps

@Composable
internal fun OverlaySettings(settings: LauncherSettings) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    @Suppress("UNUSED_VARIABLE") val recompose = tick
    val on = MatAccessibilityService.isEnabled(ctx)
    Card {
        Text(
            "Optional. With Mat Launcher's accessibility service on, the control center and lock screen can show on top of other apps. " +
                "It doesn't read your screen or what you type.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Accessibility service", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (on) "On" else "Off. Turn on Mat Launcher in Installed apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!on) FilledTonalButton(onClick = { openSystemScreen(ctx, android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) }) { Text("Open") }
        }
        SwitchRow("Control center in every app (swipe down from the top)", settings.ccEverywhere) { settings.ccEverywhere = it }
        if (settings.ccEverywhere) {
            Label("Swipe zone")
            Chips(EdgeZone.values().toList(), settings.ccZone, { it.label }) { settings.ccZone = it }
        }
        SwitchRow("Lock screen over other apps", settings.lockEverywhere) { settings.lockEverywhere = it }
        if ((settings.ccEverywhere || settings.lockEverywhere) && !on) {
            Text(
                "These do nothing until the accessibility service is on. If Android says \"Restricted setting\", see the help under Permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
