package com.matlauncher.app.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** Reads and toggles the bits of system state the control center shows. */
class SystemControls(private val ctx: Context) {
    private val cam = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var torchId: String? = null

    var torchOn by mutableStateOf(false)
        private set
    var netLabel by mutableStateOf("Off")
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var airplaneOn by mutableStateOf(false)
        private set
    var rotationAuto by mutableStateOf(true)
        private set
    var dndOn by mutableStateOf(false)
        private set
    var locationOn by mutableStateOf(false)
        private set
    var volume by mutableFloatStateOf(0.5f)
        private set
    var brightness by mutableFloatStateOf(0.5f)
        private set
    var battery by mutableIntStateOf(0)
        private set
    var charging by mutableStateOf(false)
        private set

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) torchOn = enabled
        }
    }

    fun start() {
        torchId = try {
            cam.cameraIdList.firstOrNull {
                cam.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
        try {
            cam.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            // ignore
        }
        refresh()
    }

    fun stop() {
        try {
            cam.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setTorch(on: Boolean): Boolean {
        val id = torchId ?: return false
        return try {
            cam.setTorchMode(id, on)
            torchOn = on
            true
        } catch (e: Exception) {
            false
        }
    }

    fun refresh() {
        try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            netLabel = when {
                caps == null -> "Off"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                else -> "On"
            }
        } catch (e: Exception) {
            netLabel = "Unknown"
        }
        val cr = ctx.contentResolver
        bluetoothOn = Settings.Global.getInt(cr, "bluetooth_on", 0) == 1
        airplaneOn = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        rotationAuto = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        try {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val f = nm.currentInterruptionFilter
            dndOn = f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } catch (e: Exception) {
            dndOn = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationOn = try {
                ctx.getSystemService(LocationManager::class.java).isLocationEnabled
            } catch (e: Exception) {
                false
            }
        }
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        brightness = try {
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) {
            0.5f
        }
        val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (b != null) {
            val level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0) battery = (level * 100f / scale).roundToInt()
            val status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    fun setVolume(v: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt(), 0)
        volume = v
    }

    /** Returns false (and opens the permission screen) when WRITE_SETTINGS isn't granted yet. */
    fun setBrightness(v: Float): Boolean {
        if (!Settings.System.canWrite(ctx)) {
            openWriteSettings()
            return false
        }
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (v * 255).roundToInt().coerceIn(1, 255))
        brightness = v
        return true
    }

    fun toggleRotation() {
        if (!Settings.System.canWrite(ctx)) {
            openWriteSettings()
            return
        }
        Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (rotationAuto) 0 else 1)
        refresh()
    }

    fun toggleDnd() {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            return
        }
        nm.setInterruptionFilter(
            if (dndOn) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_NONE
        )
        refresh()
    }

    fun openInternetPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) open(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        else open(Settings.ACTION_WIFI_SETTINGS)
    }

    fun openBluetooth() = open(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openAirplane() = open(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    fun openLocation() = open(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    fun openSystemSettings() = open(Settings.ACTION_SETTINGS)

    private fun openWriteSettings() {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun open(action: String) {
        try {
            ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // ignore
        }
    }

    /** Pulls down the system notification shade (needs EXPAND_STATUS_BAR; best effort). */
    fun expandNotifications() {
        try {
            val sbs = ctx.getSystemService("statusbar")
            sbs.javaClass.getMethod("expandNotificationsPanel").invoke(sbs)
        } catch (e: Exception) {
            // not available on this device
        }
    }
}
