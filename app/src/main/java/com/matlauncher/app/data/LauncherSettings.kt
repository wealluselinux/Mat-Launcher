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

enum class ClockStyle(val label: String) {
    STACKED("Stacked"),
    DIGITAL("Digital"),
    ANALOG("Analog"),
    MINIMAL("Minimal")
}

enum class WeatherStyle(val label: String) {
    CARD("Card"),
    PILL("Pill"),
    MINIMAL("Minimal")
}

enum class PhotoShape(val label: String) {
    ROUNDED("Rounded"),
    COOKIE("Cookie"),
    CIRCLE("Circle")
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
    var clockStyle by enumPref("clockStyle", ClockStyle.STACKED, ClockStyle.values())
    var weatherStyle by enumPref("weatherStyle", WeatherStyle.CARD, WeatherStyle.values())
    var photoShape by enumPref("photoShape", PhotoShape.ROUNDED, PhotoShape.values())
    var onboarded by boolPref("onboarded", false)

    // ---- home layout: ordered list of built-in and app widgets ----
    private var layoutRaw by stringPref("homeLayout", initialLayout())

    val homeItems: List<HomeItem> get() = HomeItem.parse(layoutRaw)
    fun setHomeItems(list: List<HomeItem>) { layoutRaw = HomeItem.join(list) }

    fun hasBuiltin(kind: HomeKind): Boolean = homeItems.any { it.kind == kind }

    fun setBuiltin(kind: HomeKind, on: Boolean) {
        val cur = homeItems
        if (on && cur.none { it.kind == kind }) setHomeItems(cur + defaultItem(kind))
        else if (!on) setHomeItems(cur.filter { it.kind != kind })
    }

    var showClock: Boolean
        get() = hasBuiltin(HomeKind.CLOCK)
        set(v) = setBuiltin(HomeKind.CLOCK, v)
    var showWeather: Boolean
        get() = hasBuiltin(HomeKind.WEATHER)
        set(v) = setBuiltin(HomeKind.WEATHER, v)
    var showPhoto: Boolean
        get() = hasBuiltin(HomeKind.PHOTO)
        set(v) = setBuiltin(HomeKind.PHOTO, v)

    fun resetLayout() { layoutRaw = initialLayout(ignoreSaved = true) }

    private fun defaultItem(kind: HomeKind): HomeItem = when (kind) {
        HomeKind.CLOCK -> HomeItem(kind, half = true)
        HomeKind.WEATHER -> HomeItem(kind, half = weatherStyle == WeatherStyle.CARD)
        else -> HomeItem(kind)
    }

    /** Fresh install: weather + clock + photos + Ask Mat AI. Upgrades keep what the user had. */
    private fun initialLayout(ignoreSaved: Boolean = false): String {
        val items = mutableListOf<HomeItem>()
        val legacy = !ignoreSaved
        if (!legacy || sp.getBoolean("showWeather", true)) items += defaultItem(HomeKind.WEATHER)
        if (!legacy || sp.getBoolean("showClock", true)) items += defaultItem(HomeKind.CLOCK)
        if (!legacy || sp.getBoolean("showPhoto", true)) items += defaultItem(HomeKind.PHOTO)
        items += defaultItem(HomeKind.AI)
        if (legacy) {
            split(sp.getString("widgetIds", "") ?: "").mapNotNull { it.toIntOrNull() }.forEach {
                items += HomeItem(HomeKind.HOSTED, it)
            }
        }
        return HomeItem.join(items)
    }

    // ---- top bar (date + weather header and the status bar) ----
    var topBarShow by boolPref("topBarShow", true)
    var topBarDate by boolPref("topBarDate", true)
    var topBarWeather by boolPref("topBarWeather", true)
    var topBarAlign by enumPref("topBarAlign", HAlign.START, HAlign.values())
    var topBarSize by enumPref("topBarSize", TextScale.MEDIUM, TextScale.values())
    var topBarDateStyle by enumPref("topBarDateStyle", DateStyle.SHORT, DateStyle.values())
    var hideStatusBar by boolPref("hideStatusBar", false)

    // ---- control center ----
    private var ccTilesRaw by stringPref("ccTiles", "INTERNET|BLUETOOTH|FLASHLIGHT|AIRPLANE|ROTATION|DND|LOCATION|AI")
    val ccTiles: List<TileId> get() = TileId.parse(ccTilesRaw)
    fun setCcTiles(list: List<TileId>) { ccTilesRaw = TileId.join(list) }
    var ccStyle by enumPref("ccStyle", CcStyle.LARGE, CcStyle.values())
    var ccShowHeader by boolPref("ccShowHeader", true)
    var ccShowBrightness by boolPref("ccShowBrightness", true)
    var ccShowVolume by boolPref("ccShowVolume", true)
    var ccShowShortcuts by boolPref("ccShowShortcuts", true)

    // ---- updates ----
    var autoUpdateCheck by boolPref("autoUpdateCheck", true)
    var lastUpdateCheck by StatePref(sp, "lastUpdateCheck", 0L, { k, v -> putLong(k, v) }, { k, d -> getLong(k, d) })

    // ---- weather ----
    var cityName by stringPref("cityName", "New York")
    var cityLat by floatPref("cityLat", 40.7128f)
    var cityLon by floatPref("cityLon", -74.006f)
    var useFahrenheit by boolPref("useF", Locale.getDefault().country == "US")

    // ---- lock ----
    var lockEnabled by boolPref("lockEnabled", false)
    var lockOnScreenOff by boolPref("lockOnScreenOff", true)
    var lockBackground by enumPref("lockBackground", LockBackground.GRADIENT, LockBackground.values())
    var lockClockStyle by enumPref("lockClockStyle", LockClockStyle.STACKED, LockClockStyle.values())
    var lockAlign by enumPref("lockAlign", HAlign.START, HAlign.values())
    var lockShowDate by boolPref("lockShowDate", true)
    var lockShowNotifs by boolPref("lockShowNotifs", true)
    var lockMaxNotifs by intPref("lockMaxNotifs", 5)
    var lockShowToggles by boolPref("lockShowToggles", true)
    private var lockTilesRaw by stringPref("lockTiles", "INTERNET|BLUETOOTH|FLASHLIGHT|AIRPLANE")
    val lockTiles: List<TileId> get() = TileId.parse(lockTilesRaw)
    fun setLockTiles(list: List<TileId>) { lockTilesRaw = TileId.join(list) }
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
