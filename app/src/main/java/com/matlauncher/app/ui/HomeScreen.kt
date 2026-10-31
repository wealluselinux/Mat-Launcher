package com.matlauncher.app.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.ClockStyle
import com.matlauncher.app.data.HomeItem
import com.matlauncher.app.data.HomeKind
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.PhotoShape
import com.matlauncher.app.data.WeatherStyle
import com.matlauncher.app.weather.WeatherData

@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
    homeSignal: Int,
    widgetHost: AppWidgetHost,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val scroll = rememberScrollState()
    val pager = rememberPagerState(pageCount = { settings.pageCount })
    LaunchedEffect(homeSignal) {
        if (pager.currentPage != 0) pager.animateScrollToPage(0)
        scroll.animateScrollTo(0)
    }
    LaunchedEffect(pager.currentPage) { onPageChanged(pager.currentPage) }
    // jump to a page the moment it's added
    var lastCount by remember { mutableIntStateOf(settings.pageCount) }
    LaunchedEffect(settings.pageCount) {
        if (settings.pageCount > lastCount) pager.animateScrollToPage(settings.pageCount - 1)
        lastCount = settings.pageCount
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { actions.lockNow() },
                    onLongPress = { actions.openHomeMenu() },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > 140f) actions.openControl()
                        else if (total < -140f) actions.openDrawer()
                    },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val pageScroll = if (page == 0) scroll else rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(pageScroll, enabled = pageScroll.maxValue > 0)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (editing) {
                    EditHeader(
                        page = page,
                        pageCount = settings.pageCount,
                        onAdd = actions.openWidgetPicker,
                        onAddPage = { settings.addPage() },
                        onRemovePage = { settings.removePage(page) },
                        onDone = { onEditingChange(false) },
                    )
                }
                if (page == 0 && settings.topBarShow && (settings.topBarDate || settings.topBarWeather)) {
                    AtAGlance(weather, settings, Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                HomeItems(settings, weather, actions, widgetHost, editing, page)

                if (page == 0) {
                    val pinnedApps = settings.pinned.mapNotNull { k -> apps.firstOrNull { it.key == k } }
                    val cols = settings.gridColumns
                    if (pinnedApps.isEmpty()) {
                        Text(
                            "Long-press the home screen to add widgets or change styles, or long-press an app in the drawer to add it here",
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.merge(OnWallpaper),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    } else {
                        pinnedApps.chunked(cols).take(3).forEach { row ->
                            Row(Modifier.fillMaxWidth()) {
                                row.forEach { app ->
                                    AppCell(app, settings, actions, Modifier.weight(1f))
                                }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else if (settings.homeItems.none { settings.pageOf(it) == page } && !editing) {
                    Text(
                        "This page is empty. Long-press the home screen, then Edit home, to add widgets here.",
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.merge(OnWallpaper),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (settings.pageCount > 1) {
                Row(Modifier.padding(bottom = 8.dp)) {
                    repeat(settings.pageCount) { i ->
                        val active = i == pager.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (active) 18.dp else 6.dp, 6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (active) 0.95f else 0.5f))
                        )
                    }
                }
            }
            Dock(apps, settings, actions)
            Spacer(Modifier.height(10.dp))
            SearchPill(actions)
        }

        // A strip along the top that always opens the control center, even when the page scrolls.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 60f) actions.openControl() },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { _, dy -> total += dy },
                    )
                }
                .statusBarsPadding()
                .height(22.dp)
        )
    }
}

// ------------------------------------------------------------------------- layout

private class Row2(val items: List<HomeItem>) {
    val paired: Boolean get() = items.size == 2
}

private fun pairRows(items: List<HomeItem>): List<Row2> {
    val out = mutableListOf<Row2>()
    var i = 0
    while (i < items.size) {
        val a = items[i]
        val b = items.getOrNull(i + 1)
        if (a.half && b != null && b.half) {
            out += Row2(listOf(a, b)); i += 2
        } else {
            out += Row2(listOf(a)); i += 1
        }
    }
    return out
}

private fun autoHeight(item: HomeItem, paired: Boolean, settings: LauncherSettings, hostedDp: Int): Dp = when (item.kind) {
    HomeKind.CLOCK -> if (paired) 172.dp else clockHeight(settings.clockStyle)
    HomeKind.WEATHER -> if (paired) 172.dp else weatherHeight(settings.weatherStyle)
    HomeKind.PHOTO -> 260.dp
    HomeKind.AI -> 92.dp
    HomeKind.HOSTED -> hostedDp.dp
}

@Composable
private fun HomeItems(
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
    host: AppWidgetHost,
    editing: Boolean,
    page: Int,
) {
    val ctx = LocalContext.current
    val awm = remember { AppWidgetManager.getInstance(ctx) }
    val items = settings.homeItems

    // forget widgets whose app was uninstalled
    LaunchedEffect(items) {
        val gone = items.filter { it.kind == HomeKind.HOSTED && awm.getAppWidgetInfo(it.id) == null }
        if (gone.isNotEmpty()) {
            gone.forEach { try { host.deleteAppWidgetId(it.id) } catch (e: Exception) { /* ignore */ } }
            settings.setHomeItems(items - gone.toSet())
        }
    }

    var resizing by remember { mutableStateOf<HomeItem?>(null) }
    val live = items.filter { it.kind != HomeKind.HOSTED || awm.getAppWidgetInfo(it.id) != null }
    val pageItems = live.filter { settings.pageOf(it) == page }

    fun replace(old: HomeItem, new: HomeItem) {
        settings.setHomeItems(settings.homeItems.map { if (it.key == old.key) new else it })
    }

    fun move(item: HomeItem, delta: Int) {
        val l = settings.homeItems.toMutableList()
        val idxs = l.indices.filter { settings.pageOf(l[it]) == page }
        val pos = idxs.indexOfFirst { l[it].key == item.key }
        val tgt = pos + delta
        if (pos < 0 || tgt !in idxs.indices) return
        val i = idxs[pos]
        val j = idxs[tgt]
        val t = l[i]; l[i] = l[j]; l[j] = t
        settings.setHomeItems(l)
    }

    fun remove(item: HomeItem) {
        if (item.kind == HomeKind.HOSTED) {
            try { host.deleteAppWidgetId(item.id) } catch (e: Exception) { /* ignore */ }
        }
        settings.setHomeItems(settings.homeItems.filter { it.key != item.key })
    }

    pairRows(pageItems).forEach { row ->
        val infos = row.items.map { if (it.kind == HomeKind.HOSTED) awm.getAppWidgetInfo(it.id) else null }
        val heights = row.items.mapIndexed { i, it ->
            if (it.height > 0) it.height.dp
            else autoHeight(it, row.paired, settings, infos[i]?.let { inf -> hostedAutoHeightDp(ctx, inf) } ?: 160)
        }
        Row(
            Modifier.fillMaxWidth().height(heights.maxOf { it }),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.items.forEachIndexed { i, item ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ItemContent(item, infos[i], row.paired, settings, weather, actions, host)
                    if (editing) {
                        Box(
                            Modifier
                                .matchParentSizeCompat()
                                .clip(RoundedCornerShape(28.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        )
                        EditBar(
                            Modifier.align(Alignment.BottomCenter).padding(8.dp),
                            onUp = { move(item, -1) },
                            onDown = { move(item, 1) },
                            onResize = { resizing = item },
                            onRemove = { remove(item) },
                        )
                    }
                }
            }
        }
    }

    resizing?.let { item0 ->
        val current = live.firstOrNull { it.key == item0.key }
        if (current == null) {
            resizing = null
        } else {
            val info = if (current.kind == HomeKind.HOSTED) awm.getAppWidgetInfo(current.id) else null
            val paired = pairRows(live.filter { settings.pageOf(it) == settings.pageOf(current) }).firstOrNull { r -> r.items.any { it.key == current.key } }?.paired == true
            val autoDp = autoHeight(current, paired, settings, info?.let { hostedAutoHeightDp(ctx, it) } ?: 160).value.toInt()
            ResizeDialog(
                item = current,
                autoDp = autoDp,
                settings = settings,
                onChange = { replace(current, it) },
                onDismiss = { resizing = null },
            )
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxSize()

@Composable
private fun ItemContent(
    item: HomeItem,
    info: AppWidgetProviderInfo?,
    paired: Boolean,
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
    host: AppWidgetHost,
) {
    when (item.kind) {
        HomeKind.CLOCK -> ClockWidget(settings, Modifier.fillMaxSize(), compact = paired)
        HomeKind.WEATHER -> WeatherWidget(weather, settings, onClick = actions.openWeatherApp, modifier = Modifier.fillMaxSize())
        HomeKind.PHOTO -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PhotoWidget(
                settings,
                onAdd = actions.pickPhotos,
                modifier = if (settings.photoShape == PhotoShape.ROUNDED) Modifier.fillMaxSize()
                else Modifier.fillMaxHeight().aspectRatio(1f),
            )
        }
        HomeKind.AI -> AiCard(actions, Modifier.fillMaxSize())
        HomeKind.HOSTED -> if (info != null) HostedWidgetView(host, item.id, info, Modifier.fillMaxSize())
    }
}

// ------------------------------------------------------------------------- edit mode

@Composable
private fun EditHeader(
    page: Int,
    pageCount: Int,
    onAdd: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Edit home · page ${page + 1} of $pageCount", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Move, resize or remove widgets. Swipe sideways to switch pages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onDone) { Text("Done") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Text(" Widget")
                }
                FilledTonalButton(onClick = onAddPage, enabled = pageCount < LauncherSettings.MAX_PAGES) { Text("+ Page") }
                if (pageCount > 1) TextButton(onClick = onRemovePage) { Text("Remove page") }
            }
        }
    }
}

@Composable
private fun EditBar(modifier: Modifier, onUp: () -> Unit, onDown: () -> Unit, onResize: () -> Unit, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(50), color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            BarButton(Icons.Rounded.KeyboardArrowUp, "Move up", onUp)
            BarButton(Icons.Rounded.KeyboardArrowDown, "Move down", onDown)
            BarButton(Icons.Rounded.OpenInFull, "Resize", onResize)
            BarButton(Icons.Rounded.Close, "Remove", onRemove)
        }
    }
}

@Composable
private fun BarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon, desc,
        Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(7.dp),
    )
}

@Composable
private fun ResizeDialog(
    item: HomeItem,
    autoDp: Int,
    settings: LauncherSettings,
    onChange: (HomeItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val effective = if (item.height > 0) item.height else autoDp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.kind == HomeKind.HOSTED) "Resize widget" else "Resize ${item.kind.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (settings.pageCount > 1) {
                    Label("Page")
                    Chips((0 until settings.pageCount).toList(), settings.pageOf(item), { "Page ${it + 1}" }) { onChange(item.copy(page = it)) }
                }
                Label("Width")
                Chips(listOf(true, false), item.half, { if (it) "Half" else "Full" }) { onChange(item.copy(half = it)) }
                Label("Height: ${if (item.height > 0) "$effective dp" else "Auto ($autoDp dp)"}")
                Slider(
                    value = effective.toFloat().coerceIn(60f, 560f),
                    onValueChange = { onChange(item.copy(height = it.toInt())) },
                    valueRange = 60f..560f,
                )
                TextButton(onClick = { onChange(item.copy(height = 0)) }) { Text("Reset height to auto") }
                when (item.kind) {
                    HomeKind.CLOCK -> {
                        Label("Style")
                        Chips(ClockStyle.values().toList(), settings.clockStyle, { it.label }) { settings.clockStyle = it }
                    }
                    HomeKind.WEATHER -> {
                        Label("Style")
                        Chips(WeatherStyle.values().toList(), settings.weatherStyle, { it.label }) { settings.weatherStyle = it }
                    }
                    HomeKind.PHOTO -> {
                        Label("Shape")
                        Chips(PhotoShape.values().toList(), settings.photoShape, { it.label }) { settings.photoShape = it }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ------------------------------------------------------------------------- bits

@Composable
private fun AiCard(actions: LauncherActions, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = actions.openAi,
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = scheme.tertiaryContainer,
        contentColor = scheme.onTertiaryContainer,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(CookieShape).background(scheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(26.dp), tint = scheme.onTertiary)
            }
            Column {
                Text("Ask Mat AI", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Open apps, check the weather, do math, restyle your icons",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Dock(apps: List<AppInfo>, settings: LauncherSettings, actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    val dockApps = settings.dock.mapNotNull { k -> apps.firstOrNull { it.key == k } }
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = scheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dockApps.forEach { app ->
                AppCell(app, settings, actions, Modifier.size(70.dp), showLabel = false, iconSize = 54.dp)
            }
            if (dockApps.isEmpty()) {
                FilledTonalIconButton(onClick = actions.openDrawer, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Rounded.Apps, "App drawer")
                }
            }
        }
    }
}

/** Pill search bar like the Pixel launcher's: tap to search apps, sparkle to ask Mat AI. */
@Composable
private fun SearchPill(actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = actions.openDrawer,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(50),
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Search, null, Modifier.size(24.dp), tint = scheme.primary)
            Text("Search apps", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.Rounded.AutoAwesome,
                "Ask Mat AI",
                Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable { actions.openAi() },
                tint = scheme.primary,
            )
        }
    }
}
