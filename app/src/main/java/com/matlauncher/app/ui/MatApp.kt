package com.matlauncher.app.ui

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.matlauncher.app.MainActivity
import com.matlauncher.app.ai.AiHost
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.util.ImageTools
import com.matlauncher.app.util.SystemControls
import com.matlauncher.app.util.WallpaperTools
import com.matlauncher.app.util.WeatherInstaller
import com.matlauncher.app.weather.WeatherApi
import com.matlauncher.app.weather.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Overlay { None, Drawer, Control, Ai, Settings, HomeMenu }

@Composable
fun MatApp(activity: MainActivity) {
    val settings = activity.settings
    val repo = activity.repo
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var overlay by remember { mutableStateOf(Overlay.None) }
    var aiQuery by remember { mutableStateOf<String?>(null) }
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var showInstall by remember { mutableStateOf(false) }
    var showWallpapers by remember { mutableStateOf(false) }
    var weatherInstalled by remember { mutableStateOf(WeatherInstaller.isInstalled(ctx)) }
    var pendingIconApp by remember { mutableStateOf<AppInfo?>(null) }
    val sys = remember { SystemControls(ctx) }

    DisposableEffect(Unit) {
        sys.start()
        onDispose { sys.stop() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        weatherInstalled = WeatherInstaller.isInstalled(ctx)
    }

    // ---- data loading
    LaunchedEffect(activity.reloadTick.intValue) { repo.load() }

    LaunchedEffect(repo.apps) {
        if (repo.apps.isNotEmpty() && !settings.seeded) {
            settings.setDock(repo.defaultDockKeys().take(4))
            settings.seeded = true
        }
    }

    LaunchedEffect(settings.cityLat, settings.cityLon) {
        weather = null
        while (true) {
            val w = WeatherApi.fetch(settings.cityLat.toDouble(), settings.cityLon.toDouble())
            if (w != null) weather = w
            delay(30 * 60_000L)
        }
    }

    LaunchedEffect(Unit) {
        if (!settings.weatherPrompted && !WeatherInstaller.isInstalled(ctx)) {
            delay(1500)
            showInstall = true
            settings.weatherPrompted = true
        }
    }

    LaunchedEffect(activity.homeSignal.intValue) { overlay = Overlay.None }

    // ---- pickers
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val ok = WallpaperTools.setFromUri(ctx, uri)
            Toast.makeText(ctx, if (ok) "Wallpaper updated" else "Couldn't set wallpaper", Toast.LENGTH_SHORT).show()
        }
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val app = pendingIconApp
        if (uri != null && app != null) scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageTools.decodeUri(ctx, uri, 512)?.let { ImageTools.centerCropSquare(it, 256) }
            }
            if (bmp != null) settings.saveCustomIcon(app.key, bmp)
            else Toast.makeText(ctx, "Couldn't load that image", Toast.LENGTH_SHORT).show()
        }
        pendingIconApp = null
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            withContext(Dispatchers.IO) {
                uris.forEach { u -> ImageTools.decodeUri(ctx, u, 1600)?.let { settings.addPhoto(it) } }
            }
        }
    }

    fun openWeatherApp() {
        if (WeatherInstaller.isInstalled(ctx)) {
            val i = ctx.packageManager.getLaunchIntentForPackage(WeatherInstaller.PKG)
            if (i != null) {
                i.putExtra("city", settings.cityName)
                i.putExtra("lat", settings.cityLat.toDouble())
                i.putExtra("lon", settings.cityLon.toDouble())
                i.putExtra("fahrenheit", settings.useFahrenheit)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            }
        } else {
            showInstall = true
        }
    }

    val actions = remember {
        LauncherActions(
            launch = { app ->
                overlay = Overlay.None
                activity.launchApp(app)
            },
            openDrawer = { overlay = Overlay.Drawer },
            openControl = { overlay = Overlay.Control },
            openHomeMenu = { overlay = Overlay.HomeMenu },
            openAi = { aiQuery = null; overlay = Overlay.Ai },
            openSettings = { overlay = Overlay.Settings },
            closeOverlay = { overlay = Overlay.None },
            lockNow = {
                if (settings.lockEnabled) {
                    overlay = Overlay.None
                    activity.locked.value = true
                } else {
                    Toast.makeText(ctx, "Enable the Mat lock screen in settings first", Toast.LENGTH_SHORT).show()
                    overlay = Overlay.Settings
                }
            },
            pickPhotos = { photoPicker.launch("image/*") },
            pickIconFor = { app -> pendingIconApp = app; iconPicker.launch("image/*") },
            pickWallpaper = { wallpaperPicker.launch("image/*") },
            showWallpaperGallery = { showWallpapers = true },
            openWeatherApp = { openWeatherApp() },
            installWeather = { WeatherInstaller.install(ctx) },
            setDefaultLauncher = {
                try {
                    if (Build.VERSION.SDK_INT >= 29) {
                        val rm = ctx.getSystemService(RoleManager::class.java)
                        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                            activity.startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                        } else {
                            Toast.makeText(ctx, "Mat Launcher is already your home app", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Open Settings > Apps > Default apps > Home app", Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    val aiHost = AiHost(
        apps = repo.apps,
        settings = settings,
        weather = weather,
        battery = { sys.refresh(); sys.battery },
        launch = { app -> overlay = Overlay.None; activity.launchApp(app) },
        setTorch = { sys.setTorch(it) },
        openWallpaper = { overlay = Overlay.None; showWallpapers = true },
        openSettings = { overlay = Overlay.Settings },
        lockNow = { actions.lockNow() },
        openUrl = { url ->
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // no browser
            }
        },
    )

    val locked by activity.locked

    BackHandler(enabled = locked || overlay != Overlay.None) {
        if (!locked) overlay = Overlay.None
    }

    MatTheme(seed = activity.wallpaperSeed.value) {
        Box(Modifier.fillMaxSize()) {
            HomeScreen(repo.apps, settings, weather, actions, activity.homeSignal.intValue)

            AnimatedVisibility(
                visible = overlay == Overlay.Drawer,
                enter = slideInVertically(tween(260)) { it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(),
            ) {
                AppDrawer(repo.apps, settings, actions, onAskAi = { q -> aiQuery = q; overlay = Overlay.Ai })
            }

            AnimatedVisibility(
                visible = overlay == Overlay.Control,
                enter = slideInVertically(tween(260)) { -it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { -it } + fadeOut(),
            ) {
                ControlCenter(sys, actions)
            }

            AnimatedVisibility(
                visible = overlay == Overlay.Ai,
                enter = slideInVertically(tween(260)) { it / 2 } + fadeIn(),
                exit = fadeOut(),
            ) {
                AiChat(aiHost, aiQuery, onClose = { overlay = Overlay.None })
            }

            AnimatedVisibility(
                visible = overlay == Overlay.Settings,
                enter = slideInVertically(tween(260)) { it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(),
            ) {
                SettingsScreen(repo.apps, settings, actions, weatherInstalled, onBack = { overlay = Overlay.None })
            }

            AnimatedVisibility(
                visible = overlay == Overlay.HomeMenu,
                enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(),
                exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(),
            ) {
                HomeMenu(settings, actions)
            }

            AnimatedVisibility(
                visible = locked,
                enter = fadeIn(tween(200)),
                exit = slideOutVertically(tween(300)) { -it } + fadeOut(),
            ) {
                LockScreen(locked, settings, onUnlock = { activity.locked.value = false })
            }

            if (showWallpapers) {
                WallpaperGalleryDialog(
                    onPick = { p ->
                        showWallpapers = false
                        scope.launch {
                            val ok = WallpaperTools.setPreset(ctx, p)
                            Toast.makeText(ctx, if (ok) "Wallpaper set: ${p.name}" else "Couldn't set wallpaper", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFromGallery = { showWallpapers = false; wallpaperPicker.launch("image/*") },
                    onDismiss = { showWallpapers = false },
                )
            }

            if (showInstall) {
                AlertDialog(
                    onDismissRequest = { showInstall = false },
                    title = { Text("Install Mat Weather?") },
                    text = {
                        Text("Mat Launcher comes with Mat Weather, a Material You weather app. Android will ask you to confirm the install.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showInstall = false; WeatherInstaller.install(ctx) }) { Text("Install") }
                    },
                    dismissButton = { TextButton(onClick = { showInstall = false }) { Text("Not now") } },
                )
            }
        }
    }
}
