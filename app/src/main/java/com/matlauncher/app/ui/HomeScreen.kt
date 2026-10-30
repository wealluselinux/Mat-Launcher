package com.matlauncher.app.ui

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.PhotoShape
import com.matlauncher.app.data.WeatherStyle
import com.matlauncher.app.weather.WeatherData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
    homeSignal: Int,
    widgetHost: AppWidgetHost,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    LaunchedEffect(homeSignal) {
        if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> HomePage(apps, settings, weather, actions)
                1 -> TodayPage(settings, actions)
                else -> WidgetsPage(settings, widgetHost)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.padding(bottom = 8.dp)) {
                repeat(3) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 18.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (active) 0.95f else 0.5f))
                    )
                }
            }
            Dock(apps, settings, actions)
            Spacer(Modifier.height(10.dp))
            SearchPill(actions)
        }
    }
}

@Composable
private fun HomePage(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
) {
    val pinnedApps = settings.pinned.mapNotNull { k -> apps.firstOrNull { it.key == k } }
    val cols = settings.gridColumns
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 215.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AtAGlance(weather, settings, Modifier.padding(start = 8.dp, bottom = 4.dp))

        if (settings.weatherStyle == WeatherStyle.CARD) {
            if (settings.showClock || settings.showWeather) {
                Row(
                    Modifier.fillMaxWidth().height(172.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (settings.showWeather) {
                        WeatherWidget(weather, settings, onClick = actions.openWeatherApp, modifier = Modifier.weight(1.25f).fillMaxSize())
                    }
                    if (settings.showClock) {
                        ClockWidget(settings, Modifier.weight(1f).fillMaxSize(), compact = true)
                    }
                }
            }
        } else {
            if (settings.showWeather) {
                WeatherWidget(
                    weather, settings, onClick = actions.openWeatherApp,
                    modifier = Modifier.fillMaxWidth().height(weatherHeight(settings.weatherStyle)),
                )
            }
            if (settings.showClock) {
                ClockWidget(settings, Modifier.fillMaxWidth().height(clockHeight(settings.clockStyle)))
            }
        }

        if (pinnedApps.isEmpty()) {
            Text(
                "Long-press the home screen for style options, or long-press an app in the drawer to add it here",
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
    }
}

@Composable
private fun TodayPage(settings: LauncherSettings, actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 215.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settings.showPhoto) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PhotoWidget(
                    settings,
                    onAdd = actions.pickPhotos,
                    modifier = if (settings.photoShape == PhotoShape.ROUNDED) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Surface(
            onClick = actions.openAi,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
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
                Modifier.size(24.dp).clip(CircleShape).clickable { actions.openAi() },
                tint = scheme.primary,
            )
        }
    }
}
