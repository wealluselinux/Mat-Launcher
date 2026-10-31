package com.matlauncher.app.ui

import android.content.Context
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.util.Updater

/** One feature introduced in [since], shown on the "what's new" screen after an update. */
class NewFeature(
    val since: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val toggleLabel: String? = null,
    val get: ((LauncherSettings) -> Boolean)? = null,
    val set: ((LauncherSettings, Boolean) -> Unit)? = null,
    val actionLabel: String? = null,
    val action: ((Context) -> Unit)? = null,
)

private val openAccessibility: (Context) -> Unit = { openSystemScreen(it, Settings.ACTION_ACCESSIBILITY_SETTINGS) }

/** Add the new features of each release here; the screen shows everything newer than the version you last saw. */
val NEW_FEATURES = listOf(
    NewFeature(
        "1.5.0", "Top bar on every page",
        "The date and weather header can stay put while you swipe between home pages, like the dock.",
        Icons.Rounded.ViewCarousel,
        toggleLabel = "Keep top bar on every page", get = { it.topBarAllPages }, set = { s, v -> s.topBarAllPages = v },
    ),
    NewFeature(
        "1.5.0", "Control center over other apps",
        "Swipe down from the top edge of any app to open the control center. It needs Mat Launcher's accessibility service, which reads nothing from your screen.",
        Icons.Rounded.Tune,
        toggleLabel = "Control center in every app", get = { it.ccEverywhere }, set = { s, v -> s.ccEverywhere = v },
        actionLabel = "Open accessibility settings", action = openAccessibility,
    ),
    NewFeature(
        "1.5.0", "Lock screen over other apps",
        "Show the Mat lock screen on top of whatever app is open when the screen turns off. It sits below Android's own lock. Uses the same accessibility service.",
        Icons.Rounded.Lock,
        toggleLabel = "Lock screen over other apps", get = { it.lockEverywhere }, set = { s, v -> s.lockEverywhere = v },
        actionLabel = "Open accessibility settings", action = openAccessibility,
    ),
    NewFeature(
        "1.6.0", "Settings in categories",
        "Settings now opens to a list of categories (Home & widgets, Lock screen, Control center and more). Tap one to see just its options.",
        Icons.Rounded.AutoAwesome,
    ),
    NewFeature(
        "1.6.0", "Drag apps to rearrange",
        "Long-press the home screen, choose Edit home, then long-press an app and drag it. Hold it at the screen edge to move it to another page.",
        Icons.Rounded.OpenWith,
    ),
    NewFeature(
        "1.6.0", "Folders",
        "In edit mode, drop an app onto the middle of another app to make a folder. Tap a folder to open it, rename it, or take apps back out.",
        Icons.Rounded.Folder,
    ),
)

fun newFeaturesSince(lastSeen: String): List<NewFeature> =
    NEW_FEATURES.filter { Updater.isNewer(it.since, lastSeen) }

/** Walks through what changed after an update, with switches to turn the new things on or off. */
@Composable
fun WhatsNew(settings: LauncherSettings, version: String, lastSeen: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val features = remember(lastSeen) { newFeaturesSince(lastSeen) }
    var index by remember { mutableIntStateOf(0) }
    val f = features.getOrNull(index)

    Surface(Modifier.fillMaxSize(), color = scheme.surface) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UpdateBanner(version)
            Text("What's new", style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (f != null) {
                    Surface(shape = RoundedCornerShape(28.dp), color = scheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(20.dp), color = scheme.primaryContainer) {
                                Icon(f.icon, null, Modifier.padding(14.dp).size(28.dp), tint = scheme.onPrimaryContainer)
                            }
                            Text(f.title, style = MaterialTheme.typography.titleLarge)
                            Text(f.body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                            if (f.toggleLabel != null && f.get != null && f.set != null) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(f.toggleLabel, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Switch(checked = f.get.invoke(settings), onCheckedChange = { f.set.invoke(settings, it) })
                                }
                            }
                            if (f.actionLabel != null && f.action != null) {
                                FilledTonalButton(onClick = { f.action.invoke(ctx) }) { Text(f.actionLabel) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                features.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == index) 18.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (i == index) scheme.primary else scheme.outlineVariant)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDone) { Text("Skip") }
                Spacer(Modifier.weight(1f))
                if (index > 0) TextButton(onClick = { index-- }) { Text("Back") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (index >= features.size - 1) onDone() else index++ }) {
                    Text(if (index >= features.size - 1) "Done" else "Next")
                }
            }
        }
    }
}
