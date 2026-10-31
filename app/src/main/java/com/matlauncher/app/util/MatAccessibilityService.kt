package com.matlauncher.app.util

import android.accessibilityservice.AccessibilityService
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.matlauncher.app.MainActivity
import com.matlauncher.app.data.EdgeZone
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.ui.ControlCenter
import com.matlauncher.app.ui.LauncherActions
import com.matlauncher.app.ui.LockScreen
import com.matlauncher.app.ui.MatTheme
import kotlin.math.max

/**
 * Optional accessibility service. It reads nothing from the screen; it only lets Mat Launcher add
 * two overlay windows on top of other apps:
 *  - a thin strip at the top edge that opens the control center when you swipe down on it
 *  - the lock screen, shown when the screen turns off
 * Both are off until switched on in Mat Launcher's settings.
 */
class MatAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: MatAccessibilityService? = null
        /** Set by the launcher activity: when Mat Launcher is on screen the strip isn't needed. */
        @Volatile var launcherVisible = false

        fun refresh() {
            val s = instance ?: return
            s.handler.post { s.refreshStrip() }
        }

        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val cn = ComponentName(ctx, MatAccessibilityService::class.java)
            return enabled.split(':').any {
                it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var ccHost: OverlayHost
    private lateinit var lockHost: OverlayHost
    private var stripView: View? = null
    private var stripZone: EdgeZone? = null
    private var sys: SystemControls? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        handler.post { refreshStrip() }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val s = LauncherSettings(applicationContext)
            if (s.lockEverywhere && s.lockEnabled && s.lockOnScreenOff) showLock()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ccHost = OverlayHost(this, wm)
        lockHost = OverlayHost(this, wm)
        getSharedPreferences("mat_launcher", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefListener)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshStrip()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* nothing to read */ }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        removeStrip()
        refreshStrip()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        if (instance == null) return
        instance = null
        try { getSharedPreferences("mat_launcher", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener) } catch (e: Exception) { /* ignore */ }
        try { unregisterReceiver(screenReceiver) } catch (e: Exception) { /* ignore */ }
        removeStrip()
        ccHost.hide()
        lockHost.hide()
        sys?.stop()
        sys = null
    }

    // ------------------------------------------------------------------ top-edge strip

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    fun refreshStrip() {
        if (instance == null) return
        val s = LauncherSettings(applicationContext)
        val want = s.ccEverywhere && !launcherVisible && !ccHost.showing && !lockHost.showing
        when {
            !want -> removeStrip()
            stripView == null || stripZone != s.ccZone -> { removeStrip(); addStrip(s.ccZone) }
        }
    }

    private fun addStrip(zone: EdgeZone) {
        val dm = resources.displayMetrics
        val h = max(statusBarHeight(), (24 * dm.density).toInt())
        val w = if (zone == EdgeZone.FULL) WindowManager.LayoutParams.MATCH_PARENT else dm.widthPixels / 2
        val lp = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or (if (zone == EdgeZone.FULL) Gravity.START else Gravity.END) }

        val threshold = 60 * dm.density
        var downY = 0f
        var fired = false
        val v = View(this)
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; fired = false }
                MotionEvent.ACTION_MOVE -> if (!fired && e.rawY - downY > threshold) {
                    fired = true
                    handler.post { showControlCenter() }
                }
            }
            true
        }
        try {
            wm.addView(v, lp)
            stripView = v
            stripZone = zone
        } catch (e: Exception) {
            // overlay not allowed right now
        }
    }

    private fun removeStrip() {
        val v = stripView ?: return
        try { wm.removeView(v) } catch (e: Exception) { /* already gone */ }
        stripView = null
        stripZone = null
    }

    // ------------------------------------------------------------------ overlays

    private fun fullScreenParams(): WindowManager.LayoutParams {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return lp
    }

    private fun wallpaperSeed(): Int? = try {
        if (Build.VERSION.SDK_INT >= 27) {
            WallpaperManager.getInstance(this).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
        } else null
    } catch (e: Exception) {
        null
    }

    private fun openMat(overlay: String) {
        try {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra("mat_overlay", overlay)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open Mat Launcher", Toast.LENGTH_SHORT).show()
        }
    }

    private fun controls(): SystemControls {
        val c = sys ?: SystemControls(this).also { sys = it }
        c.start()
        return c
    }

    private fun hideControlCenter() {
        ccHost.hide()
        if (!lockHost.showing) { sys?.stop(); sys = null }
        refreshStrip()
    }

    private fun showControlCenter() {
        if (ccHost.showing || lockHost.showing) return
        val settings = LauncherSettings(applicationContext)
        val c = controls()
        removeStrip()
        val close = { handler.post { hideControlCenter() }; Unit }
        val actions = LauncherActions(
            launch = {}, openDrawer = {}, openControl = {}, openHomeMenu = {}, openWidgetPicker = {}, editHome = {},
            openAi = { close(); openMat("ai") },
            openSettings = { close(); openMat("settings") },
            closeOverlay = { close() },
            lockNow = {
                close()
                if (settings.lockEnabled) handler.post { showLock() }
                else Toast.makeText(this, "Turn on the Mat lock screen in settings first", Toast.LENGTH_SHORT).show()
            },
            pickPhotos = {}, pickIconFor = { _ -> }, pickWallpaper = {},
            showWallpaperGallery = { close(); openMat("wallpaper") },
            openWeatherApp = {
                close()
                packageManager.getLaunchIntentForPackage(WeatherInstaller.PKG)?.let {
                    startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            installWeather = {}, setDefaultLauncher = {},
        )
        ccHost.show(fullScreenParams()) {
            MatTheme(seed = wallpaperSeed()) {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                AnimatedVisibility(visible = shown, enter = slideInVertically { -it } + fadeIn()) {
                    ControlCenter(c, settings, actions)
                }
            }
        }
    }

    private fun hideLock() {
        lockHost.hide()
        if (!ccHost.showing) { sys?.stop(); sys = null }
        refreshStrip()
    }

    fun showLock() {
        if (lockHost.showing) return
        val settings = LauncherSettings(applicationContext)
        if (!settings.lockEnabled) return
        ccHost.hide()
        removeStrip()
        val c = controls()
        lockHost.show(fullScreenParams()) {
            MatTheme(seed = wallpaperSeed()) {
                LockScreen(locked = true, settings = settings, sys = c, onUnlock = { handler.post { hideLock() } })
            }
        }
    }
}
