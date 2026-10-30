package com.matlauncher.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.ClockStyle
import com.matlauncher.app.data.IconShape
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.PhotoShape
import com.matlauncher.app.data.WeatherStyle

private const val LAST_STEP = 5

/** First-run setup. Every choice here can be changed later in Settings. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Onboarding(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    actions: LauncherActions,
    weatherInstalled: Boolean,
    onFinish: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var step by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var cityDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }

    fun go(to: Int) {
        forward = to > step
        step = to
    }

    Surface(Modifier.fillMaxSize(), color = scheme.surface) {
        Box(Modifier.fillMaxSize()) {
            // soft decorative blobs
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp)
                    .size(220.dp)
                    .clip(CookieShape)
                    .background(scheme.primaryContainer.copy(alpha = 0.6f))
            )
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(320)) { it * dir } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320)) { -it * dir } + fadeOut(tween(200)))
                    },
                    label = "onboarding",
                ) { s ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (s) {
                            0 -> Welcome()
                            1 -> LookStep(apps, settings)
                            2 -> WidgetsStep(settings) { cityDialog = true }
                            3 -> LockStep(settings) { pinDialog = true }
                            4 -> PermissionsStep()
                            else -> FinishStep(settings, actions, weatherInstalled)
                        }
                    }
                }

                // bottom bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step > 0) TextButton(onClick = { go(step - 1) }) { Text("Back") } else Spacer(Modifier.size(64.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                        repeat(LAST_STEP + 1) { i ->
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (i == step) 20.dp else 7.dp, 7.dp)
                                    .clip(CircleShape)
                                    .background(if (i == step) scheme.primary else scheme.onSurface.copy(alpha = 0.2f))
                            )
                        }
                    }
                    if (step < LAST_STEP) {
                        Button(onClick = { go(step + 1) }) { Text(if (step == 0) "Get started" else "Next") }
                    } else {
                        Button(onClick = onFinish) { Text("Finish") }
                    }
                }
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
}

@Composable
private fun Welcome() {
    val scheme = MaterialTheme.colorScheme
    Spacer(Modifier.height(48.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(150.dp).clip(CookieShape).background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Home, null, Modifier.size(80.dp), tint = scheme.onPrimary)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Welcome to Mat Launcher",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "A Material You home screen that follows your wallpaper. Let's set it up the way you like it. It only takes a minute, and everything can be changed later.",
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepTitle(title: String, body: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LookStep(apps: List<AppInfo>, settings: LauncherSettings) {
    StepTitle("App icons", "Pick a style and a shape. The preview updates live.")
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            apps.take(4).forEach { AppIcon(it, settings, 60.dp) }
        }
    }
    Card {
        Label("Style")
        Chips(IconStyle.values().toList(), settings.iconStyle, { it.label }) { settings.iconStyle = it }
        Label("Shape")
        Chips(IconShape.values().toList(), settings.iconShape, { it.label }) { settings.iconShape = it }
    }
}

@Composable
private fun WidgetsStep(settings: LauncherSettings, onCity: () -> Unit) {
    StepTitle("Widgets", "Choose how the built-in clock, weather and photo widgets look. You can also add widgets from other apps later.")
    Card {
        SwitchRow("Clock widget", settings.showClock) { settings.showClock = it }
        Label("Clock style")
        Chips(ClockStyle.values().toList(), settings.clockStyle, { it.label }) { settings.clockStyle = it }
    }
    Card {
        SwitchRow("Weather widget", settings.showWeather) { settings.showWeather = it }
        Label("Weather style")
        Chips(WeatherStyle.values().toList(), settings.weatherStyle, { it.label }) { settings.weatherStyle = it }
        SwitchRow("Fahrenheit", settings.useFahrenheit) { settings.useFahrenheit = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Location: ${settings.cityName}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            FilledTonalButton(onClick = onCity) { Text("Change") }
        }
    }
    Card {
        SwitchRow("Photo widget", settings.showPhoto) { settings.showPhoto = it }
        Label("Photo shape")
        Chips(PhotoShape.values().toList(), settings.photoShape, { it.label }) { settings.photoShape = it }
    }
}

@Composable
private fun LockStep(settings: LauncherSettings, onPin: () -> Unit) {
    StepTitle("Lock screen", "Mat's lock screen shows a big clock, your notifications and quick toggles. It sits on top of the home screen and does not replace your phone's system lock.")
    Card {
        SwitchRow("Use Mat lock screen", settings.lockEnabled) { settings.lockEnabled = it }
        SwitchRow("Lock when the screen turns off", settings.lockOnScreenOff) { settings.lockOnScreenOff = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (settings.hasPin) "4-digit PIN is set" else "No PIN (swipe up to unlock)",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            FilledTonalButton(onClick = onPin) { Text(if (settings.hasPin) "Change" else "Set PIN") }
        }
    }
}

@Composable
private fun PermissionsStep() {
    StepTitle("Permissions", "Nothing here is required. Each one unlocks a feature, and Grant opens the right Android screen.")
    Card { PermissionRows() }
}

@Composable
private fun FinishStep(settings: LauncherSettings, actions: LauncherActions, weatherInstalled: Boolean) {
    StepTitle("Almost done", "Pick a wallpaper, install the weather app, and make Mat Launcher your home app.")
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = actions.showWallpaperGallery) { Text("Choose wallpaper") }
            FilledTonalButton(onClick = actions.pickWallpaper) { Text("From gallery") }
        }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (weatherInstalled) "Mat Weather is installed" else "Install Mat Weather (Material You weather app)",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!weatherInstalled) FilledTonalButton(onClick = actions.installWeather) { Text("Install") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Make Mat Launcher your home app", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            FilledTonalButton(onClick = actions.setDefaultLauncher) { Text("Set") }
        }
    }
}
