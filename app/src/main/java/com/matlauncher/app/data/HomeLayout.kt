package com.matlauncher.app.data

/** The things that can sit on the home screen, in order. */
enum class HomeKind(val label: String) {
    CLOCK("Clock"),
    WEATHER("Weather"),
    PHOTO("Photos"),
    AI("Ask Mat AI"),
    HOSTED("App widget"),
}

/**
 * One block on the home screen. [half] blocks pair up side by side; [height] is in dp and 0
 * means "automatic" (the widget's natural height). [id] is the AppWidget id for HOSTED items.
 */
data class HomeItem(
    val kind: HomeKind,
    val id: Int = 0,
    val half: Boolean = false,
    val height: Int = 0,
    val page: Int = 0,
) {
    val key: String get() = "${kind.name}:$id"

    fun encode(): String = "${kind.name},$id,${if (half) 1 else 0},$height,$page"

    companion object {
        private fun decode(s: String): HomeItem? {
            val p = s.split(",")
            if (p.size < 4) return null
            val kind = HomeKind.values().firstOrNull { it.name == p[0] } ?: return null
            val id = p[1].toIntOrNull() ?: return null
            return HomeItem(kind, id, p[2] == "1", p[3].toIntOrNull() ?: 0, p.getOrNull(4)?.toIntOrNull() ?: 0)
        }

        fun parse(raw: String): List<HomeItem> =
            raw.split("|").filter { it.isNotBlank() }.mapNotNull { decode(it) }

        fun join(list: List<HomeItem>): String = list.joinToString("|") { it.encode() }
    }
}

enum class DateStyle(val label: String, val pattern: String) {
    SHORT("Short", "EEE, d MMM"),
    LONG("Long", "EEEE, MMMM d"),
    NUMERIC("Numeric", "M/d/yyyy"),
    DAY("Weekday", "EEEE"),
}

enum class HAlign(val label: String) {
    START("Left"),
    CENTER("Center"),
}

enum class TextScale(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
}

enum class LockBackground(val label: String) {
    GRADIENT("Gradient"),
    SOLID("Solid"),
    WALLPAPER("Wallpaper (dimmed)"),
}

enum class LockClockStyle(val label: String) {
    STACKED("Stacked"),
    INLINE("One line"),
    ANALOG("Analog"),
}

enum class EdgeZone(val label: String) {
    RIGHT("Top right"),
    FULL("Full width"),
}

enum class CcStyle(val label: String) {
    LARGE("Large tiles"),
    COMPACT("Compact icons"),
}

/** Quick settings tiles that can be shown in the control center and on the lock screen. */
enum class TileId(val label: String, val onLock: Boolean) {
    INTERNET("Internet", true),
    BLUETOOTH("Bluetooth", true),
    FLASHLIGHT("Flashlight", true),
    AIRPLANE("Airplane mode", true),
    ROTATION("Auto-rotate", true),
    DND("Do not disturb", true),
    LOCATION("Location", true),
    SOUND("Sound mode", true),
    AI("Mat AI", false);

    companion object {
        fun parse(raw: String): List<TileId> =
            raw.split("|").mapNotNull { n -> values().firstOrNull { it.name == n } }.distinct()

        fun join(list: List<TileId>): String = list.joinToString("|") { it.name }
    }
}
