package com.matlauncher.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.matlauncher.app.util.NotificationStore
import com.matlauncher.app.util.ShellAccess

/** Opens a system screen where the user grants a special permission. */
fun openSystemScreen(ctx: Context, action: String, withPackage: Boolean = false) {
    try {
        val i = Intent(action)
        if (withPackage) i.data = Uri.parse("package:${ctx.packageName}")
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // ignore
    }
}

/**
 * The permissions Mat Launcher can use. None are required: every row says what it unlocks.
 * Special permissions can't be granted with a popup on Android, so "Grant" opens the right system screen.
 */
@Composable
fun PermissionRows() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val refreshKey = tick
    val notif = NotificationStore.hasAccess(ctx)
    val write = Settings.System.canWrite(ctx)
    val dnd = try {
        ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    } catch (e: Exception) {
        false
    }
    val install = ctx.packageManager.canRequestPackageInstalls()
    val shizukuInstalled = ShellAccess.installed(ctx)
    val shizukuRunning = ShellAccess.running()
    val shizukuGranted = ShellAccess.granted()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionRow(
            "Notification access", "Show notifications on the lock screen", notif,
        ) { openSystemScreen(ctx, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
        PermissionRow(
            "Modify system settings", "Brightness and auto-rotate in the control center", write,
        ) { openSystemScreen(ctx, Settings.ACTION_MANAGE_WRITE_SETTINGS, true) }
        PermissionRow(
            "Do Not Disturb access", "Toggle Do Not Disturb from the control center", dnd,
        ) { openSystemScreen(ctx, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) }
        PermissionRow(
            "Install apps", "Install the bundled Mat Weather app and updates", install,
        ) { openSystemScreen(ctx, Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, true) }

        // Wi-Fi / Bluetooth / airplane / mobile data can only be switched through Shizuku.
        val shizukuSub = when {
            shizukuGranted -> "Mat AI and the tiles can switch Wi-Fi, Bluetooth, airplane mode and mobile data"
            shizukuRunning -> "Shizuku is running. Allow Mat Launcher to use it"
            shizukuInstalled -> "Shizuku is installed but not started. Open it and start the service"
            else -> "Optional: lets Mat AI and the tiles switch Wi-Fi, Bluetooth and airplane mode"
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Shizuku (system toggles)", style = MaterialTheme.typography.bodyLarge)
                Text(shizukuSub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                shizukuGranted -> Icon(Icons.Rounded.CheckCircle, "Granted", Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                shizukuRunning -> FilledTonalButton(onClick = { ShellAccess.requestPermission() }) { Text("Allow") }
                shizukuInstalled -> FilledTonalButton(onClick = { ShellAccess.openShizuku(ctx) }) { Text("Open") }
                else -> FilledTonalButton(onClick = { ShellAccess.openDownloadPage(ctx) }) { Text("Get") }
            }
        }

        RestrictedSettingsHelp(ctx)
        ShizukuHelp()
    }
}

/**
 * Apps installed from a file (not from a store) are "restricted" by Android: notification access and
 * a few other special permissions are greyed out until the user allows it once in App info.
 */
@Composable
private fun RestrictedSettingsHelp(ctx: Context) {
    var open by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Permission greyed out or \"Restricted setting\"?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Android blocks some permissions for apps installed from a file. You allow it once.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { open = !open }) { Text(if (open) "Hide" else "Show steps") }
            }
            if (open) {
                Text(
                    "1. Tap Grant on Notification access above and try switching Mat Launcher on. Android says it's a restricted setting. Tap OK.\n" +
                        "2. Tap Open app info below.\n" +
                        "3. Tap the ⋮ menu in the top-right corner, then Allow restricted settings, and confirm with your PIN or fingerprint.\n" +
                        "4. Go back and switch the permission on.\n\n" +
                        "If you don't see the ⋮ menu, do step 1 first. It only appears after Android has blocked something once.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = { openSystemScreen(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true) }) {
                    Text("Open app info")
                }
            }
        }
    }
}

@Composable
private fun ShizukuHelp() {
    var open by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Why can't Mat AI switch Wi-Fi by itself?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Android doesn't allow normal apps to. Shizuku is the no-root workaround.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { open = !open }) { Text(if (open) "Hide" else "How") }
            }
            if (open) {
                Text(
                    "Modify system settings and Do Not Disturb access cover brightness, rotation, sound modes and Do Not Disturb. " +
                        "They do not cover Wi-Fi, Bluetooth, airplane mode or mobile data; those are locked to the system.\n\n" +
                        "To unlock them: install the free Shizuku app, turn on Developer options and Wireless debugging, open Shizuku and tap Start (pairing), then come back here and tap Allow. " +
                        "Shizuku has to be started again after every reboot. Without it, those tiles just open the system panel.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Icon(Icons.Rounded.CheckCircle, "Granted", Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(onClick = onGrant) { Text("Grant") }
        }
    }
}
