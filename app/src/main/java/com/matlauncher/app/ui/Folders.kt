package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.HomeFolder
import com.matlauncher.app.data.LauncherSettings

/** An app or a folder on the home grid. */
@Composable
fun EntryCell(
    entry: HomeEntry,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    actions: LauncherActions,
    modifier: Modifier,
    onOpenFolder: (String) -> Unit,
) {
    if (entry.app != null) AppCell(entry.app, settings, actions, modifier)
    else if (entry.folder != null) FolderCell(entry.folder, apps, settings, modifier) { onOpenFolder(entry.key) }
}

/** The folder icon: a rounded tile with up to four mini app icons, plus its name. */
@Composable
fun FolderCell(folder: HomeFolder, apps: List<AppInfo>, settings: LauncherSettings, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val inside = folder.apps.mapNotNull { k -> apps.firstOrNull { it.key == k } }.take(4)
    val size = (56f * settings.iconScale).dp
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(Color.White.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                inside.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { AppIcon(it, settings, 22.dp) }
                    }
                }
            }
        }
        if (settings.showLabels) {
            Text(
                folder.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.merge(OnWallpaper),
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The opened folder: rename it, launch its apps, or take apps back out onto the home screen. */
@Composable
fun FolderDialog(folder: HomeFolder, apps: List<AppInfo>, settings: LauncherSettings, actions: LauncherActions, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    var removing by remember { mutableStateOf(false) }
    val inside = folder.apps.mapNotNull { k -> apps.firstOrNull { it.key == k } }
    val close = { settings.renameFolder(folder.id, name); onDismiss() }
    // launching an app pauses the launcher: close the folder with it
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { close() }

    Dialog(onDismissRequest = close) {
        Surface(shape = RoundedCornerShape(32.dp), color = scheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (inside.isEmpty()) {
                    Text("This folder is empty.", color = scheme.onSurfaceVariant)
                }
                inside.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            Box(Modifier.weight(1f)) {
                                AppCell(app, settings, actions, Modifier, labelOnSurface = true)
                                if (removing) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(scheme.errorContainer)
                                            .clickable { settings.removeFromFolder(folder.id, app.key) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Close, "Take out of folder", Modifier.size(16.dp), tint = scheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { removing = !removing }) { Text(if (removing) "Done" else "Take apps out") }
                    TextButton(onClick = close) { Text("Close") }
                }
            }
        }
    }
}
