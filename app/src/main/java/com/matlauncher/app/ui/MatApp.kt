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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalView
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.HomeItem
import com.matlauncher.app.data.HomeKind
import com.matlauncher.app.util.UpdateState
import com.matlauncher.app.util.Updater
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

enum class Overlay { None, Drawer, Control, Ai, Settings, HomeMenu, Widgets }

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
    var launching by remember { mutableStateOf<LaunchRequest?>(null) }
    val sys = remember { SystemControls(ctx) }
    var editing by remember { mutableStateOf(false) }
    var homePage by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val updater = remember { Updater() }
    var updateNotice by remember { mutableStateOf(false) }

    val addHosted = rememberWidgetAdder(activity.widgetHost) { id, _ ->
        settings.setHomeItems(settings.homeItems + HomeItem(HomeKind.HOSTED, id, page = homePage))
        overlay = Overlay.None
        editing = true
    }

    // status bar: optionally hidden for a cleaner home screen
    val view = LocalView.current
    LaunchedEffect(settings.hideStatusBar) {
        val c = WindowCompat.getInsetsController(activity.window, view)
        if (settings.hideStatusBar) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            c.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // quiet update check at most twice a day
    LaunchedEffect(Unit) {
        if (settings.autoUpdateCheck && System.currentTimeMillis() - settings.lastUpdateCheck > 12 * 3_600_000L) {
            delay(4000)
            settings.lastUpdateCheck = System.currentTimeMillis()
            updater.check(ctx)
            if (updater.state is UpdateState.Available) updateNotice = true
        }
    }

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

    // screens requested by the over-other-apps control center
    val pending = activity.pendingOverlay.value
    LaunchedEffect(pending) {
        when (pending) {
            "ai" -> { aiQuery = null; overlay = Overlay.Ai }
            "settings" -> overlay = Overlay.Settings
            "wallpaper" -> showWallpapers = true
        }
        if (pending != null) activity.pendingOverlay.value = null
    }

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
                if (launching == null) {
                    val origin = LaunchOrigin.consume()
                    overlay = Overlay.None
                    launching = LaunchRequest(app, origin)
                }
            },
            openDrawer = { overlay = Overlay.Drawer },
            openControl = { overlay = Overlay.Control },
            openHomeMenu = { overlay = Overlay.HomeMenu },
            openWidgetPicker = { overlay = Overlay.Widgets },
            editHome = { overlay = Overlay.None; editing = true },
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
        launch = { app -> actions.launch(app) },
        setTorch = { sys.setTorch(it) },
        openWallpaper = { overlay = Overlay.None; showWallpapers = true },
        openSettings = { overlay = Overlay.Settings },
        lockNow = { actions.lockNow() },
        sys = sys,
        openUrl = { url ->
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // no browser
            }
        },
    )

    val locked by activity.locked

    val onboarded = settings.onboarded
    BackHandler(enabled = locked || overlay != Overlay.None || !onboarded || editing) {
        if (!locked) {
            if (overlay != Overlay.None) overlay = Overlay.None else editing = false
        }
    }

    val homeScale by animateFloatAsState(if (onboarded) 1f else 0.92f, tween(650), label = "homeScale")
    val homeAlpha by animateFloatAsState(if (onboarded) 1f else 0f, tween(650), label = "homeAlpha")

    MatTheme(seed = activity.wallpaperSeed.value) {
        Box(Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize().graphicsLayer { scaleX = homeScale; scaleY = homeScale; alpha = homeAlpha }) {
            HomeScreen(
                repo.apps, settings, weather, actions, activity.homeSignal.intValue, activity.widgetHost,
                editing = editing, onEditingChange = { editing = it },
                onPageChanged = { homePage = it },
            )

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
                ControlCenter(sys, settings, actions)
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
                SettingsScreen(repo.apps, settings, actions, weatherInstalled, updater, onBack = { overlay = Overlay.None })
            }

            AnimatedVisibility(
                visible = overlay == Overlay.HomeMenu,
                enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(),
                exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(),
            ) {
                HomeMenu(settings, actions)
            }

            AnimatedVisibility(
                visible = overlay == Overlay.Widgets,
                enter = slideInVertically(tween(260)) { it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(),
            ) {
                WidgetPicker(
                    settings = settings,
                    onClose = { overlay = Overlay.None },
                    onPickHosted = { info -> addHosted(info) },
                    onPickBuiltin = { kind ->
                        settings.setBuiltin(kind, true, homePage)
                        overlay = Overlay.None
                        editing = true
                    },
                )
            }

            AnimatedVisibility(
                visible = locked,
                enter = fadeIn(tween(200)),
                exit = slideOutVertically(tween(300)) { -it } + fadeOut(),
            ) {
                LockScreen(locked, settings, sys, onUnlock = { activity.locked.value = false })
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
            if (updateNotice) {
                val info = (updater.state as? UpdateState.Available)?.info
                AlertDialog(
                    onDismissRequest = { updateNotice = false },
                    title = { Text("Update available") },
                    text = { Text("Mat Launcher ${info?.version ?: ""} is ready. Open Settings → Updates to install it.") },
                    confirmButton = {
                        TextButton(onClick = { updateNotice = false; overlay = Overlay.Settings }) { Text("Open settings") }
                    },
                    dismissButton = { TextButton(onClick = { updateNotice = false }) { Text("Later") } },
                )
            }
            launching?.let { req ->
                LaunchAnimation(
                    req,
                    onStart = { app, rect -> activity.launchApp(app, rect) },
                    onDone = { launching = null },
                )
            }
          }

            AnimatedVisibility(
                visible = !onboarded,
                enter = fadeIn(),
                exit = fadeOut(tween(650)) + scaleOut(tween(650), targetScale = 1.12f),
            ) {
                Onboarding(
                    apps = repo.apps,
                    settings = settings,
                    actions = actions,
                    weatherInstalled = weatherInstalled,
                    onFinish = { settings.onboarded = true },
                )
            }
        }
    }
}
