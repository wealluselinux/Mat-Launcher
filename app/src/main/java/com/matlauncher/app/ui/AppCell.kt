package com.matlauncher.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.LauncherSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(
    app: AppInfo,
    settings: LauncherSettings,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
    showLabel: Boolean = settings.showLabels,
    labelColor: Color = Color.White,
    iconSize: Dp = 56.dp,
    labelOnSurface: Boolean = false,
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val label = settings.labelFor(app.key, app.label)

    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .onGloballyPositioned { coords = it }
                .combinedClickable(
                    onClick = {
                        LaunchOrigin.rect = try { coords?.boundsInRoot() } catch (e: Exception) { null }
                        actions.launch(app)
                    },
                    onLongClick = { menu = true },
                )
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(app, settings, iconSize)
            if (showLabel) {
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = if (labelOnSurface) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.labelMedium.merge(OnWallpaper),
                    color = if (labelOnSurface) MaterialTheme.colorScheme.onSurface else labelColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val pinned = app.key in settings.pinned
            val inDock = app.key in settings.dock
            DropdownMenuItem(
                text = { Text(if (pinned) "Remove from home" else "Add to home") },
                leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                onClick = {
                    menu = false
                    settings.setPinned(if (pinned) settings.pinned - app.key else settings.pinned + app.key)
                },
            )
            DropdownMenuItem(
                text = { Text(if (inDock) "Remove from dock" else "Add to dock") },
                leadingIcon = { Icon(Icons.Rounded.Apps, null) },
                onClick = {
                    menu = false
                    if (inDock) settings.setDock(settings.dock - app.key)
                    else if (settings.dock.size >= 4) Toast.makeText(ctx, "The dock is full (4 apps)", Toast.LENGTH_SHORT).show()
                    else settings.setDock(settings.dock + app.key)
                },
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menu = false; renaming = true },
            )
            DropdownMenuItem(
                text = { Text("Change icon") },
                leadingIcon = { Icon(Icons.Rounded.Image, null) },
                onClick = { menu = false; actions.pickIconFor(app) },
            )
            if (settings.customIconFile(app.key).exists()) {
                DropdownMenuItem(
                    text = { Text("Reset icon") },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                    onClick = { menu = false; settings.clearCustomIcon(app.key) },
                )
            }
            DropdownMenuItem(
                text = { Text("Hide") },
                leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) },
                onClick = { menu = false; settings.setHidden(settings.hidden + app.key) },
            )
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                onClick = {
                    menu = false
                    try {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = {
                    menu = false
                    try {
                        ctx.startActivity(
                            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Can't uninstall this app", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }

    if (renaming) {
        var text by remember { mutableStateOf(label) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename app") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.rename(app.key, if (text.trim() == app.label) null else text)
                    renaming = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }
}
