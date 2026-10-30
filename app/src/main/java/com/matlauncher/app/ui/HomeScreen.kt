package com.matlauncher.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.matlauncher.app.weather.WeatherData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    weather: WeatherData?,
    actions: LauncherActions,
    homeSignal: Int,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(homeSignal) {
        if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { actions.lockNow() })
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
            if (page == 0) HomePage(apps, settings, weather, actions) else TodayPage(settings, actions)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.padding(bottom = 10.dp)) {
                repeat(2) { i ->
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
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settings.showClock) ClockWidget(settings)
        if (settings.showWeather) WeatherWidget(weather, settings, onClick = actions.openWeatherApp)

        if (pinnedApps.isEmpty()) {
            Text(
                "Long-press an app in the drawer and choose \"Add to home\"",
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.merge(OnWallpaper),
                color = Color.White.copy(alpha = 0.85f),
            )
        } else {
            Spacer(Modifier.height(4.dp))
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
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settings.showPhoto) {
            PhotoWidget(settings, onAdd = actions.pickPhotos, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        Surface(
            onClick = actions.openAi,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = scheme.tertiaryContainer.copy(alpha = 0.92f),
            contentColor = scheme.onTertiaryContainer,
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(28.dp))
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
        shape = RoundedCornerShape(34.dp),
        color = scheme.surfaceContainer.copy(alpha = 0.88f),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            dockApps.forEach { app ->
                AppCell(app, settings, actions, Modifier.size(68.dp), showLabel = false, iconSize = 52.dp)
            }
            FilledTonalIconButton(onClick = actions.openDrawer, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Apps, "App drawer")
            }
        }
    }
}
