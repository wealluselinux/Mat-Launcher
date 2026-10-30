package com.matlauncher.app.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class IconShape(val label: String) {
    CIRCLE("Circle"),
    SQUIRCLE("Squircle"),
    ROUNDED("Rounded"),
    TEARDROP("Teardrop"),
    NONE("None")
}

enum class IconStyle(val label: String) {
    ORIGINAL("Original"),
    THEMED("Match wallpaper"),
    TRANSPARENT("Transparent"),
    GLASS("Glass")
}

/** A SharedPreferences-backed value that is also Compose state. */
class StatePref<T>(
    private val sp: SharedPreferences,
    private val key: String,
    default: T,
    private val save: SharedPreferences.Editor.(String, T) -> Unit,
    load: SharedPreferences.(String, T) -> T,
) : ReadWriteProperty<Any?, T> {
    private val state = mutableStateOf(sp.load(key, default))

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        sp.edit().also { it.save(key, value) }.apply()
    }
}

class LauncherSettings(private val context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("mat_launcher", Context.MODE_PRIVATE)

    private fun boolPref(key: String, def: Boolean) =
        StatePref(sp, key, def, { k, v -> putBoolean(k, v) }, { k, d -> getBoolean(k, d) })

    private fun intPref(key: String, def: Int) =
        StatePref(sp, key, def, { k, v -> putInt(k, v) }, { k, d -> getInt(k, d) })

    private fun floatPref(key: String, def: Float) =
        StatePref(sp, key, def, { k, v -> putFloat(k, v) }, { k, d -> getFloat(k, d) })

    private fun stringPref(key: String, def: String) =
        StatePref(sp, key, def, { k, v -> putString(k, v) }, { k, d -> getString(k, d) ?: d })

    private fun <E : Enum<E>> enumPref(key: String, def: E, values: Array<E>) =
        StatePref(
            sp, key, def,
            { k, v -> putString(k, v.name) },
            { k, d -> values.firstOrNull { it.name == getString(k, d.name) } ?: d }
        )

    // ---- icons ----
    var iconShape by enumPref("iconShape", IconShape.SQUIRCLE, IconShape.values())
    var iconStyle by enumPref("iconStyle", IconStyle.THEMED, IconStyle.values())
    var iconScale by floatPref("iconScale", 1f)
    var showLabels by boolPref("showLabels", true)
    var gridColumns by intPref("gridColumns", 4)

    // ---- home ----
    var showClock by boolPref("showClock", true)
    var showWeather by boolPref("showWeather", true)
    var showPhoto by boolPref("showPhoto", true)
    var clockAnalog by boolPref("clockAnalog", false)

    // ---- weather ----
    var cityName by stringPref("cityName", "New York")
    var cityLat by floatPref("cityLat", 40.7128f)
    var cityLon by floatPref("cityLon", -74.006f)
    var useFahrenheit by boolPref("useF", Locale.getDefault().country == "US")

    // ---- lock ----
    var lockEnabled by boolPref("lockEnabled", false)
    var lockOnScreenOff by boolPref("lockOnScreenOff", true)
    private var pinHash by stringPref("pinHash", "")
    val hasPin: Boolean get() = pinHash.isNotEmpty()

    fun setPin(pin: String) {
        pinHash = if (pin.isEmpty()) "" else sha(pin)
    }

    fun checkPin(pin: String): Boolean = pinHash.isNotEmpty() && sha(pin) == pinHash

    private fun sha(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("mat:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---- first run flags ----
    var seeded by boolPref("seeded", false)
    var weatherPrompted by boolPref("weatherPrompted", false)

    // ---- pinned / dock / hidden ----
    private var pinnedRaw by stringPref("pinned", "")
    private var dockRaw by stringPref("dock", "")
    private var hiddenRaw by stringPref("hidden", "")

    val pinned: List<String> get() = split(pinnedRaw)
    val dock: List<String> get() = split(dockRaw)
    val hidden: Set<String> get() = split(hiddenRaw).toSet()

    fun setPinned(list: List<String>) { pinnedRaw = list.joinToString("|") }
    fun setDock(list: List<String>) { dockRaw = list.joinToString("|") }
    fun setHidden(list: Collection<String>) { hiddenRaw = list.joinToString("|") }

    private fun split(raw: String): List<String> = raw.split("|").filter { it.isNotEmpty() }

    // ---- renamed labels ----
    private var renames by mutableStateOf(loadRenames())

    private fun loadRenames(): Map<String, String> = try {
        val o = JSONObject(sp.getString("renames", "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    } catch (e: Exception) {
        emptyMap()
    }

    fun labelFor(key: String, default: String): String = renames[key] ?: default

    fun rename(key: String, label: String?) {
        val m = renames.toMutableMap()
        if (label.isNullOrBlank()) m.remove(key) else m[key] = label.trim()
        renames = m
        sp.edit().putString("renames", JSONObject(m as Map<*, *>).toString()).apply()
    }

    // ---- custom icons ----
    var iconVersion by mutableIntStateOf(0)

    private val iconDir: File get() = File(context.filesDir, "icons").also { it.mkdirs() }

    fun customIconFile(key: String): File = File(iconDir, key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".png")

    fun saveCustomIcon(key: String, bmp: Bitmap) {
        FileOutputStream(customIconFile(key)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        iconVersion++
    }

    fun clearCustomIcon(key: String) {
        customIconFile(key).delete()
        iconVersion++
    }

    // ---- photo widget ----
    private var photosRaw by stringPref("photos", "")
    private val photoDir: File get() = File(context.filesDir, "photos").also { it.mkdirs() }

    val photos: List<File>
        get() = split(photosRaw).map { File(photoDir, it) }.filter { it.exists() }

    fun addPhoto(bmp: Bitmap) {
        val name = "p${System.nanoTime()}.jpg"
        FileOutputStream(File(photoDir, name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        photosRaw = (split(photosRaw) + name).joinToString("|")
    }

    fun removePhoto(file: File) {
        file.delete()
        photosRaw = split(photosRaw).filter { it != file.name }.joinToString("|")
    }
}
