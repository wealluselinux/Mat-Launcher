package com.matlauncher.app.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.matlauncher.app.data.LauncherSettings

/**
 * The "Widgets" page: hosts real Android app widgets from any installed app.
 * Flow: pick a provider -> bind (system asks permission once) -> optional configure screen -> shown here.
 */
@Composable
fun WidgetsPage(settings: LauncherSettings, host: AppWidgetHost) {
    val ctx = LocalContext.current
    val awm = remember { AppWidgetManager.getInstance(ctx) }
    val scheme = MaterialTheme.colorScheme
    var picker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var pendingId by remember { mutableIntStateOf(-1) }

    fun addWidget(id: Int) {
        settings.setWidgetIds(settings.widgetIds + id)
        pendingId = -1
    }

    fun discard(id: Int) {
        if (id != -1) {
            try { host.deleteAppWidgetId(id) } catch (e: Exception) { /* ignore */ }
        }
        pendingId = -1
    }

    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        if (id != -1) {
            if (r.resultCode == Activity.RESULT_OK) addWidget(id) else discard(id)
        }
    }

    fun configureOrAdd(id: Int, info: AppWidgetProviderInfo) {
        val cfg = info.configure
        if (cfg != null) {
            try {
                configureLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(cfg)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
            } catch (e: Exception) {
                // can't open its configuration screen: add it with defaults
                addWidget(id)
            }
        } else {
            addWidget(id)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        if (id != -1) {
            val info = awm.getAppWidgetInfo(id)
            if (r.resultCode == Activity.RESULT_OK && info != null) configureOrAdd(id, info) else discard(id)
        }
    }

    fun startAdd(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        val allowed = try {
            awm.bindAppWidgetIdIfAllowed(id, info.provider)
        } catch (e: Exception) {
            false
        }
        if (allowed) {
            configureOrAdd(id, info)
        } else {
            try {
                bindLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                )
            } catch (e: Exception) {
                Toast.makeText(ctx, "Couldn't add that widget", Toast.LENGTH_SHORT).show()
                discard(id)
            }
        }
    }

    val ids = settings.widgetIds

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 215.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Widgets",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.merge(OnWallpaper),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (ids.isNotEmpty()) {
                IconButton(onClick = { editing = !editing }) {
                    Icon(if (editing) Icons.Rounded.Close else Icons.Rounded.Edit, "Edit", tint = Color.White)
                }
            }
            FilledTonalButton(onClick = { picker = true }) {
                Icon(Icons.Rounded.Add, null)
                Text("  Add")
            }
        }

        if (ids.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Add widgets from any app you have installed.\nTap Add to get started.",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.merge(OnWallpaper),
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ids.forEach { id ->
                    val info = awm.getAppWidgetInfo(id)
                    if (info == null) {
                        // provider was uninstalled: forget it
                        settings.setWidgetIds(settings.widgetIds - id)
                    } else {
                        HostedWidget(host, id, info, editing) {
                            discard(id)
                            settings.setWidgetIds(settings.widgetIds - id)
                        }
                    }
                }
            }
        }
    }

    if (picker) {
        val pm = ctx.packageManager
        val providers = remember(picker) {
            awm.installedProviders.sortedBy { it.loadLabel(pm)?.lowercase() ?: "" }
        }
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("Add a widget") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (providers.isEmpty()) Text("No widgets found on this device.")
                    providers.forEach { info ->
                        val app = try {
                            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                        } catch (e: Exception) {
                            info.provider.packageName
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { picker = false; startAdd(info) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(info.loadLabel(pm) ?: app, style = MaterialTheme.typography.bodyLarge)
                            Text(app, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HostedWidget(
    host: AppWidgetHost,
    id: Int,
    info: AppWidgetProviderInfo,
    editing: Boolean,
    onRemove: () -> Unit,
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val heightDp = with(density) { info.minHeight.toDp() }.coerceAtLeast(80.dp)
    val widthDp = with(density) { info.minWidth.toDp() }

    Box(Modifier.fillMaxWidth().height(heightDp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                factory = {
                    host.createView(ctx, id, info).apply {
                        setAppWidget(id, info)
                    }
                },
                update = { view ->
                    val w = widthDp.value.toInt()
                    val h = heightDp.value.toInt()
                    view.updateAppWidgetSize(null, w, h, w, h)
                },
            )
        }
        if (editing) {
            Surface(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Remove widget", Modifier.size(20.dp)) }
            }
        }
    }
}
