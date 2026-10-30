package com.matlauncher.app

import android.app.ActivityOptions
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.AppRepository
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.ui.MatApp

class MainActivity : ComponentActivity() {
    lateinit var settings: LauncherSettings
    lateinit var repo: AppRepository

    val locked = mutableStateOf(false)
    val homeSignal = mutableIntStateOf(0)
    val reloadTick = mutableIntStateOf(0)
    val wallpaperSeed = mutableStateOf<Int?>(null)

    private var wm: WallpaperManager? = null
    private var colorsListener: Any? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (settings.lockEnabled && settings.lockOnScreenOff) locked.value = true
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadTick.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        settings = LauncherSettings(applicationContext)
        repo = AppRepository(applicationContext)
        locked.value = settings.lockEnabled

        ContextCompat.registerReceiver(
            this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, pkgFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        wm = WallpaperManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= 27) {
            val listener = WallpaperManager.OnColorsChangedListener { _, _ -> wallpaperSeed.value = readSeed() }
            colorsListener = listener
            wm?.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            wallpaperSeed.value = readSeed()
        }

        setContent { MatApp(this) }
    }

    private fun readSeed(): Int? =
        if (Build.VERSION.SDK_INT >= 27) wm?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
        else null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal.intValue++
    }

    @Deprecated("Back is handled by Compose")
    override fun onBackPressed() {
        // The launcher never finishes; Compose's BackHandler closes overlays.
        super.onBackPressed()
    }

    fun launchApp(app: AppInfo) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(app.pkg, app.cls))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(i, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        unregisterReceiver(packageReceiver)
        if (Build.VERSION.SDK_INT >= 27) {
            (colorsListener as? WallpaperManager.OnColorsChangedListener)?.let { wm?.removeOnColorsChangedListener(it) }
        }
    }
}
