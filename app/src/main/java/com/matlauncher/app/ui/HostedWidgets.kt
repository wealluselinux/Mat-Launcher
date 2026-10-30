package com.matlauncher.app.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.matlauncher.app.data.HomeKind
import com.matlauncher.app.data.LauncherSettings
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Adding a widget from another app: bind (the system asks permission once) -> its own
// configuration screen if it has one -> onAdded.
// ---------------------------------------------------------------------------------------------

@Composable
fun rememberWidgetAdder(
    host: AppWidgetHost,
    onAdded: (Int, AppWidgetProviderInfo) -> Unit,
): (AppWidgetProviderInfo) -> Unit {
    val ctx = LocalContext.current
    val awm = remember { AppWidgetManager.getInstance(ctx) }
    var pendingId by remember { mutableIntStateOf(-1) }
    val added by rememberUpdatedState(onAdded)

    fun discard(id: Int) {
        if (id != -1) {
            try { host.deleteAppWidgetId(id) } catch (e: Exception) { /* ignore */ }
        }
        pendingId = -1
    }

    fun finish(id: Int) {
        val info = awm.getAppWidgetInfo(id)
        pendingId = -1
        if (info != null) added(id, info) else discard(id)
    }

    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        if (id != -1) {
            if (r.resultCode == Activity.RESULT_OK) finish(id) else discard(id)
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
                finish(id) // can't open its configuration screen: add it with defaults
            }
        } else {
            finish(id)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        if (id != -1) {
            val info = awm.getAppWidgetInfo(id)
            if (r.resultCode == Activity.RESULT_OK && info != null) configureOrAdd(id, info) else discard(id)
        }
    }

    fun start(info: AppWidgetProviderInfo) {
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

    return { info -> start(info) }
}

// ---------------------------------------------------------------------------------------------
// Showing a widget from another app, at whatever size the home layout gives it.
// ---------------------------------------------------------------------------------------------

@Composable
fun HostedWidgetView(host: AppWidgetHost, id: Int, info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth.value.toInt()
            val h = maxHeight.value.toInt()
            AndroidView(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                factory = {
                    host.createView(ctx, id, info).apply { setAppWidget(id, info) }
                },
                update = { view -> view.updateAppWidgetSize(null, w, h, w, h) },
            )
        }
    }
}

/** Natural height of a widget from its provider info, in dp. */
fun hostedAutoHeightDp(ctx: Context, info: AppWidgetProviderInfo): Int {
    val d = ctx.resources.displayMetrics.density
    return (info.minHeight / d).toInt().coerceIn(80, 600)
}

// ---------------------------------------------------------------------------------------------
// Widget picker: apps on top; tap an app to expand the list of its widgets with previews.
// This is why a 300-widget app no longer buries everything else.
// ---------------------------------------------------------------------------------------------

private class AppGroup(val pkg: String, val label: String, val providers: List<AppWidgetProviderInfo>)

private data class BuiltinOption(val kind: HomeKind, val icon: ImageVector, val blurb: String)

private val builtinOptions = listOf(
    BuiltinOption(HomeKind.CLOCK, Icons.Rounded.Schedule, "Stacked, digital, analog or minimal clock"),
    BuiltinOption(HomeKind.WEATHER, Icons.Rounded.WbSunny, "Pixel-style card, pill or minimal weather"),
    BuiltinOption(HomeKind.PHOTO, Icons.Rounded.Image, "Photo frame with rounded, cookie or circle shape"),
    BuiltinOption(HomeKind.AI, Icons.Rounded.AutoAwesome, "Shortcut to the built-in Mat AI"),
)

@Composable
fun WidgetPicker(
    settings: LauncherSettings,
    onClose: () -> Unit,
    onPickHosted: (AppWidgetProviderInfo) -> Unit,
    onPickBuiltin: (HomeKind) -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val pm = ctx.packageManager
    val awm = remember { AppWidgetManager.getInstance(ctx) }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }

    val groups = remember {
        awm.installedProviders
            .groupBy { it.provider.packageName }
            .map { (pkg, list) ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }
                AppGroup(pkg, label, list.sortedBy { (it.loadLabel(pm) ?: "").lowercase() })
            }
            .sortedBy { it.label.lowercase() }
    }

    val q = query.trim().lowercase()
    val shownBuiltins = builtinOptions.filter { q.isEmpty() || it.kind.label.lowercase().contains(q) || "mat launcher".contains(q) }
    val shownGroups = if (q.isEmpty()) groups else groups.filter { g ->
        g.label.lowercase().contains(q) || g.providers.any { (it.loadLabel(pm) ?: "").lowercase().contains(q) }
    }

    Surface(Modifier.fillMaxSize(), color = scheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Text("Add a widget", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search apps or widgets") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shownBuiltins.isNotEmpty()) {
                    item(key = "hdr-mat") {
                        Text("Mat Launcher", style = MaterialTheme.typography.titleSmall, color = scheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    }
                    items(shownBuiltins, key = { "mat-" + it.kind.name }) { opt ->
                        val present = settings.hasBuiltin(opt.kind)
                        Surface(
                            onClick = { onPickBuiltin(opt.kind) },
                            enabled = !present,
                            shape = RoundedCornerShape(24.dp),
                            color = scheme.surfaceContainer,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(CookieShape).background(scheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(opt.icon, null, tint = scheme.onPrimaryContainer) }
                                Column(Modifier.weight(1f)) {
                                    Text(opt.kind.label, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (present) "Already on your home screen" else opt.blurb,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "hdr-apps") {
                    Text(
                        "From your apps",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                    )
                }
                if (shownGroups.isEmpty()) {
                    item(key = "none") {
                        Text("No matching widgets.", color = scheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                }
                shownGroups.forEach { g ->
                    val isOpen = open == g.pkg || (q.isNotEmpty() && shownGroups.size == 1)
                    item(key = "app-" + g.pkg) {
                        AppRow(g, isOpen) { open = if (open == g.pkg) null else g.pkg }
                    }
                    if (isOpen) {
                        val list = if (q.isEmpty() || g.label.lowercase().contains(q)) g.providers
                        else g.providers.filter { (it.loadLabel(pm) ?: "").lowercase().contains(q) }
                        items(list, key = { "w-" + it.provider.flattenToString() }) { info ->
                            WidgetOption(info) { onPickHosted(info) }
                        }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AppRow(g: AppGroup, open: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val icon = remember(g.pkg) {
        try {
            ctx.packageManager.getApplicationIcon(g.pkg).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (open) scheme.secondaryContainer else scheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (icon != null) Image(icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            else Spacer(Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text(g.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${g.providers.size} widget" + if (g.providers.size == 1) "" else "s",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
    }
}

@Composable
private fun WidgetOption(info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val label = remember(info) { info.loadLabel(ctx.packageManager) ?: info.provider.shortClassName }
    val preview = remember(info) { previewBitmap(ctx, info) }
    val cells = remember(info) { cellSize(ctx, info) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.padding(start = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (preview != null) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 220.dp).clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerLowest),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(preview, null, Modifier.fillMaxWidth().heightIn(max = 220.dp), contentScale = ContentScale.Fit)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Text(cells, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

private fun cellSize(ctx: Context, info: AppWidgetProviderInfo): String {
    if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
        return "${info.targetCellWidth} × ${info.targetCellHeight}"
    }
    val d = ctx.resources.displayMetrics.density
    val w = max(1, ((info.minWidth / d + 30) / 70).toInt())
    val h = max(1, ((info.minHeight / d + 30) / 70).toInt())
    return "$w × $h"
}

private fun previewBitmap(ctx: Context, info: AppWidgetProviderInfo): ImageBitmap? = try {
    val d: Drawable? = info.loadPreviewImage(ctx, 0)
    if (d == null) null else {
        val iw = if (d.intrinsicWidth > 0) d.intrinsicWidth else 320
        val ih = if (d.intrinsicHeight > 0) d.intrinsicHeight else 160
        val s = min(1f, 480f / max(iw, ih))
        d.toBitmap(max(1, (iw * s).toInt()), max(1, (ih * s).toInt())).asImageBitmap()
    }
} catch (e: Exception) {
    null
}
