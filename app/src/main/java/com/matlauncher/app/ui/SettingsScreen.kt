package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.IconShape
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.util.WallpaperPreset
import com.matlauncher.app.util.WallpaperTools
import com.matlauncher.app.util.WeatherInstaller
import com.matlauncher.app.weather.GeoResult
import com.matlauncher.app.weather.WeatherApi
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    actions: LauncherActions,
    weatherInstalled: Boolean,
    updater: com.matlauncher.app.util.Updater,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var cityDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var hiddenDialog by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = scheme.surface.copy(alpha = 0.98f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Text("Mat Launcher settings", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ---------------- Updates (top)
                UpdateBanner(updater.installedLauncherVersion(LocalContext.current))
                UpdateCard(updater, settings, weatherInstalled)

                // ---------------- Wallpaper
                Header("Wallpaper")
                Card {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = actions.pickWallpaper) { Text("From gallery") }
                        FilledTonalButton(onClick = actions.showWallpaperGallery) { Text("Material gradients") }
                    }
                    Text(
                        "Colors across the launcher follow your wallpaper (Material You).",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }

                // ---------------- Icons
                Header("Icons")
                Card {
                    Label("Style")
                    Chips(IconStyle.values().toList(), settings.iconStyle, { it.label }) { settings.iconStyle = it }
                    Text(
                        when (settings.iconStyle) {
                            IconStyle.ORIGINAL -> "Each app's own icon colors."
                            IconStyle.THEMED -> "Tinted glyphs on a plate that matches your wallpaper."
                            IconStyle.TRANSPARENT -> "Just the glyph, no background plate."
                            IconStyle.GLASS -> "Frosted translucent plate."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Label("Shape")
                    Chips(IconShape.values().toList(), settings.iconShape, { it.label }) { settings.iconShape = it }
                    Label("Size")
                    Slider(
                        value = settings.iconScale,
                        onValueChange = { settings.iconScale = it },
                        valueRange = 0.7f..1.2f,
                    )
                    Label("Grid columns")
                    Chips(listOf(4, 5), settings.gridColumns, { "$it columns" }) { settings.gridColumns = it }
                    SwitchRow("Show app names", settings.showLabels) { settings.showLabels = it }
                    Text(
                        "Tip: long-press any app to rename it or pick your own icon image.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }

                // ---------------- Home
                Header("Home & widgets")
                Card {
                    SwitchRow("Clock widget", settings.showClock) { settings.showClock = it }
                    Label("Clock style")
                    Chips(com.matlauncher.app.data.ClockStyle.values().toList(), settings.clockStyle, { it.label }) { settings.clockStyle = it }
                    SwitchRow("Weather widget", settings.showWeather) { settings.showWeather = it }
                    Label("Weather style")
                    Chips(com.matlauncher.app.data.WeatherStyle.values().toList(), settings.weatherStyle, { it.label }) { settings.weatherStyle = it }
                    SwitchRow("Photo widget", settings.showPhoto) { settings.showPhoto = it }
                    Label("Photo shape")
                    Chips(com.matlauncher.app.data.PhotoShape.values().toList(), settings.photoShape, { it.label }) { settings.photoShape = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = actions.openWidgetPicker) { Text("Add widget") }
                        FilledTonalButton(onClick = actions.editHome) { Text("Edit layout") }
                    }
                    Text(
                        "Widgets live on the home screen. Use Edit layout to move, resize or remove them. Pick any widget from any app, grouped by app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = { settings.resetLayout() }) { Text("Reset layout") }
                    SwitchRow("Fahrenheit", settings.useFahrenheit) { settings.useFahrenheit = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Weather location", style = MaterialTheme.typography.bodyLarge)
                            Text(settings.cityName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick = { cityDialog = true }) { Text("Change") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hidden apps", style = MaterialTheme.typography.bodyLarge)
                            Text("${settings.hidden.size} hidden", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick = { hiddenDialog = true }) { Text("Manage") }
                    }
                }

                Header("Top bar")
                TopBarSettings(settings)

                // ---------------- Lock
                Header("Lock screen")
                Card {
                    SwitchRow("Enable Mat lock screen", settings.lockEnabled) { settings.lockEnabled = it }
                    SwitchRow("Lock when the screen turns off", settings.lockOnScreenOff) { settings.lockOnScreenOff = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PIN", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (settings.hasPin) "4-digit PIN set" else "No PIN (swipe up to unlock)",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { pinDialog = true }) { Text(if (settings.hasPin) "Change" else "Set") }
                    }
                    Text(
                        "This is a launcher-level lock screen on top of your home screen. It does not replace the system lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }

                Header("Lock screen look")
                LockLookSettings(settings)

                Header("Control center")
                ControlCenterSettings(settings)

                Header("Over other apps")
                OverlaySettings(settings)

                // ---------------- Apps
                Header("Apps")
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mat Weather", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (weatherInstalled) "Installed" else "Material You weather app, bundled with the launcher",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (!weatherInstalled) FilledTonalButton(onClick = actions.installWeather) { Text("Install") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Default launcher", style = MaterialTheme.typography.bodyLarge)
                            Text("Make Mat Launcher your home app", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick = actions.setDefaultLauncher) { Text("Set") }
                    }
                }

                Header("Permissions")
                Card { PermissionRows() }

                Header("Welcome screen")
                Card {
                    FilledTonalButton(onClick = { settings.onboarded = false; onBack() }) { Text("Run setup again") }
                }

                Header("About")
                Card {
                    Text("Mat Launcher ${updater.installedLauncherVersion(LocalContext.current)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Open source, Material You only. github.com/wealluselinux/Mat-Launcher",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Box(Modifier.navigationBarsPadding())
            }
        }
    }

    if (cityDialog) {
        CitySearchDialog(
            onDismiss = { cityDialog = false },
            onPick = { r ->
                settings.cityName = r.name
                settings.cityLat = r.lat.toFloat()
                settings.cityLon = r.lon.toFloat()
                cityDialog = false
            },
        )
    }
    if (pinDialog) PinDialog(settings, onDismiss = { pinDialog = false })
    if (hiddenDialog) {
        val hiddenApps = apps.filter { it.key in settings.hidden }
        AlertDialog(
            onDismissRequest = { hiddenDialog = false },
            title = { Text("Hidden apps") },
            text = {
                if (hiddenApps.isEmpty()) {
                    Text("No hidden apps. Long-press an app in the drawer and choose Hide.")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        hiddenApps.forEach { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(settings.labelFor(app.key, app.label), Modifier.weight(1f))
                                TextButton(onClick = { settings.setHidden(settings.hidden - app.key) }) { Text("Unhide") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { hiddenDialog = false }) { Text("Done") } },
        )
    }
}

@Composable
internal fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
    )
}

@Composable
internal fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

@Composable
fun CitySearchDialog(onDismiss: () -> Unit, onPick: (GeoResult) -> Unit) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run() {
        if (q.isBlank()) return
        busy = true
        scope.launch {
            results = WeatherApi.search(q)
            searched = true
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    placeholder = { Text("Search a city") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FilledTonalButton(onClick = { run() }, enabled = !busy) { Text(if (busy) "Searching…" else "Search") }
                results.forEach { r ->
                    Text(
                        r.display,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(r) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (searched && results.isEmpty() && !busy) Text("No results.")
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PinDialog(settings: LauncherSettings, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a 4-digit PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                placeholder = { Text("1234") },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length == 4) {
                    settings.setPin(pin)
                    settings.lockEnabled = true
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (settings.hasPin) TextButton(onClick = { settings.setPin(""); onDismiss() }) { Text("Remove PIN") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun WallpaperGalleryDialog(onPick: (WallpaperPreset) -> Unit, onFromGallery: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallpaper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WallpaperTools.presets.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { p ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .background(
                                        Brush.linearGradient(p.colors.map { Color(it) }),
                                        RoundedCornerShape(20.dp),
                                    )
                                    .clickable { onPick(p) },
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                Text(p.name, color = Color.White, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                FilledTonalButton(onClick = onFromGallery, modifier = Modifier.fillMaxWidth()) { Text("Pick from gallery…") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
