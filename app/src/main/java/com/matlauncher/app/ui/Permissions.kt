package com.matlauncher.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.FilledTonalButton
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
            "Install apps", "Install the bundled Mat Weather app", install,
        ) { openSystemScreen(ctx, Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, true) }
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
