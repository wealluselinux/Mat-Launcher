package com.matlauncher.app.ui

import com.matlauncher.app.data.AppInfo

/** Everything the UI layers can ask the launcher shell to do. */
class LauncherActions(
    val launch: (AppInfo) -> Unit,
    val openDrawer: () -> Unit,
    val openControl: () -> Unit,
    val openHomeMenu: () -> Unit,
    val openWidgetPicker: () -> Unit,
    val editHome: () -> Unit,
    val openAi: () -> Unit,
    val openSettings: () -> Unit,
    val closeOverlay: () -> Unit,
    val lockNow: () -> Unit,
    val pickPhotos: () -> Unit,
    val pickIconFor: (AppInfo) -> Unit,
    val pickWallpaper: () -> Unit,
    val showWallpaperGallery: () -> Unit,
    val openWeatherApp: () -> Unit,
    val installWeather: () -> Unit,
    val setDefaultLauncher: () -> Unit,
)
