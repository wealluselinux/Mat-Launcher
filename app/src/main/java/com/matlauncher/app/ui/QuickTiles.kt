package com.matlauncher.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.matlauncher.app.data.TileId
import com.matlauncher.app.util.RingerMode
import com.matlauncher.app.util.SystemControls

/** One quick-settings tile, shared by the control center and the lock screen. */
class TileSpec(
    val id: TileId,
    val icon: ImageVector,
    val label: String,
    val sub: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

/**
 * [gate] wraps actions that change system settings: the control center runs them straight away,
 * the lock screen refuses them while a PIN is set.
 */
fun tileSpec(id: TileId, sys: SystemControls, onAi: () -> Unit, gate: (() -> Unit) -> Unit): TileSpec = when (id) {
    TileId.INTERNET -> TileSpec(id, Icons.Rounded.Wifi, "Internet", sys.netLabel, sys.netLabel != "Off") {
        gate { if (sys.shellReady) sys.async { sys.setWifi(!sys.wifiOn) } else sys.openInternetPanel() }
    }
    TileId.BLUETOOTH -> TileSpec(id, Icons.Rounded.Bluetooth, "Bluetooth", if (sys.bluetoothOn) "On" else "Off", sys.bluetoothOn) {
        gate { if (sys.shellReady) sys.async { sys.setBluetooth(!sys.bluetoothOn) } else sys.openBluetooth() }
    }
    TileId.FLASHLIGHT -> TileSpec(id, Icons.Rounded.FlashlightOn, "Flashlight", if (sys.torchOn) "On" else "Off", sys.torchOn) {
        sys.setTorch(!sys.torchOn)
    }
    TileId.AIRPLANE -> TileSpec(id, Icons.Rounded.AirplanemodeActive, "Airplane", if (sys.airplaneOn) "On" else "Off", sys.airplaneOn) {
        gate { if (sys.shellReady) sys.async { sys.setAirplane(!sys.airplaneOn) } else sys.openAirplane() }
    }
    TileId.ROTATION -> TileSpec(id, Icons.Rounded.ScreenRotation, "Auto-rotate", if (sys.rotationAuto) "On" else "Locked", sys.rotationAuto) {
        gate { sys.toggleRotation() }
    }
    TileId.DND -> TileSpec(id, Icons.Rounded.DoNotDisturbOn, "Do not disturb", if (sys.dndOn) "On" else "Off", sys.dndOn) {
        gate { sys.toggleDnd() }
    }
    TileId.LOCATION -> TileSpec(id, Icons.Rounded.LocationOn, "Location", if (sys.locationOn) "On" else "Off", sys.locationOn) {
        gate { sys.openLocation() }
    }
    TileId.SOUND -> {
        val icon = when (sys.ringer) {
            RingerMode.NORMAL -> Icons.Rounded.NotificationsActive
            RingerMode.VIBRATE -> Icons.Rounded.Vibration
            RingerMode.SILENT -> Icons.Rounded.NotificationsOff
        }
        TileSpec(id, icon, "Sound", sys.ringer.label, sys.ringer != RingerMode.NORMAL) {
            gate {
                sys.setRinger(
                    when (sys.ringer) {
                        RingerMode.NORMAL -> RingerMode.VIBRATE
                        RingerMode.VIBRATE -> RingerMode.SILENT
                        RingerMode.SILENT -> RingerMode.NORMAL
                    }
                )
            }
        }
    }
    TileId.AI -> TileSpec(id, Icons.Rounded.AutoAwesome, "Mat AI", "Ask anything", false) { onAi() }
}
