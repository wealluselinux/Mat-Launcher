package com.matlauncher.app.ai

import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.IconShape
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.weather.WeatherData
import com.matlauncher.app.weather.conditionLabel
import com.matlauncher.app.weather.tempString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class AiReply(val text: String, val action: (() -> Unit)? = null)

/** Everything Mat AI is allowed to touch. */
class AiHost(
    val apps: List<AppInfo>,
    val settings: LauncherSettings,
    val weather: WeatherData?,
    val battery: () -> Int,
    val launch: (AppInfo) -> Unit,
    val setTorch: (Boolean) -> Boolean,
    val openWallpaper: () -> Unit,
    val openSettings: () -> Unit,
    val lockNow: () -> Unit,
    val openUrl: (String) -> Unit,
)

/**
 * A tiny on-device assistant. It's not a real language model: it's a rule-based
 * intent matcher that can drive the launcher. Swap `reply` for an API call to make it "real".
 */
object MatAi {
    val suggestions = listOf("Open camera", "What's the weather?", "Flashlight on", "Transparent icons", "12 * (4 + 3)", "Tell me a joke")

    private val jokes = listOf(
        "Why do Android devs love Material You? It matches their mood.",
        "I would tell you a UDP joke, but you might not get it.",
        "There are 10 kinds of people: those who understand binary and those who don't.",
        "A launcher walks into a bar. The bartender says: \"Home again?\"",
    )

    fun reply(raw: String, h: AiHost): AiReply {
        val q = raw.trim()
        val l = q.lowercase(Locale.getDefault())
        if (l.isEmpty()) return AiReply("Say something and I'll try to help!")

        // math first so "what is 2+2" works
        Calc.fromText(l)?.let { return AiReply("= " + Calc.format(it)) }

        if (Regex("^(hi|hello|hey|yo|sup)\\b").containsMatchIn(l)) {
            return AiReply("Hey! I'm Mat AI, built into your launcher. Try \"open camera\", \"weather\" or \"transparent icons\".")
        }

        Regex("^(open|launch|start|run)\\s+(.+)$").find(l)?.let { m ->
            val name = m.groupValues[2].trim()
            val app = findApp(h, name)
            return if (app != null) {
                AiReply("Opening ${h.settings.labelFor(app.key, app.label)}…") { h.launch(app) }
            } else {
                AiReply("I couldn't find an app called \"$name\".")
            }
        }

        if (l.contains("flashlight") || l.contains("torch")) {
            val on = !(l.contains("off") || l.contains("disable"))
            val ok = h.setTorch(on)
            return AiReply(if (ok) (if (on) "Flashlight on." else "Flashlight off.") else "This device doesn't seem to have a flashlight.")
        }

        if (Regex("\\b(weather|temperature|forecast|rain|how (hot|cold))\\b").containsMatchIn(l)) {
            val w = h.weather ?: return AiReply("The weather hasn't loaded yet. Check your connection and try again.")
            val f = h.settings.useFahrenheit
            return AiReply(
                "${h.settings.cityName}: ${tempString(w.tempC, f)} and ${conditionLabel(w.code).lowercase()}. " +
                    "Feels like ${tempString(w.feelsC, f)}, humidity ${w.humidity}%."
            )
        }

        if (Regex("\\b(time|clock)\\b").containsMatchIn(l)) {
            return AiReply("It's " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + ".")
        }
        if (Regex("\\b(date|day is it|today)\\b").containsMatchIn(l)) {
            return AiReply("Today is " + SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) + ".")
        }
        if (l.contains("battery")) return AiReply("Your battery is at ${h.battery()}%.")

        if (l.contains("wallpaper") || l.contains("background")) {
            return AiReply("Opening wallpapers…") { h.openWallpaper() }
        }

        // icon / launcher styling
        if (l.contains("icon") || l.contains("label")) {
            val s = h.settings
            when {
                l.contains("transparent") || l.contains("no background") -> {
                    s.iconStyle = IconStyle.TRANSPARENT
                    return AiReply("Done: icons are transparent now.")
                }
                l.contains("match") || l.contains("theme") || l.contains("wallpaper") || l.contains("material") -> {
                    s.iconStyle = IconStyle.THEMED
                    return AiReply("Done: icons now match your wallpaper colors.")
                }
                l.contains("glass") -> {
                    s.iconStyle = IconStyle.GLASS
                    return AiReply("Done: glass icons.")
                }
                l.contains("original") || l.contains("default") -> {
                    s.iconStyle = IconStyle.ORIGINAL
                    return AiReply("Done: original icon colors.")
                }
                l.contains("circle") -> { s.iconShape = IconShape.CIRCLE; return AiReply("Circle icons it is.") }
                l.contains("squircle") -> { s.iconShape = IconShape.SQUIRCLE; return AiReply("Squircle icons it is.") }
                l.contains("teardrop") -> { s.iconShape = IconShape.TEARDROP; return AiReply("Teardrop icons it is.") }
                l.contains("round") -> { s.iconShape = IconShape.ROUNDED; return AiReply("Rounded-square icons it is.") }
                l.contains("hide") && l.contains("label") -> { s.showLabels = false; return AiReply("Labels hidden.") }
                l.contains("show") && l.contains("label") -> { s.showLabels = true; return AiReply("Labels shown.") }
            }
        }

        if (l == "lock" || l.contains("lock screen") || l.contains("lock my") || l.contains("lock the")) {
            return AiReply("Locking…") { h.lockNow() }
        }
        if (l.contains("setting")) return AiReply("Opening Mat Launcher settings…") { h.openSettings() }

        Regex("^(search|google|look up)( for)?\\s+(.+)$").find(l)?.let { m ->
            val term = m.groupValues[3]
            return AiReply("Searching the web for \"$term\"…") {
                h.openUrl("https://www.google.com/search?q=" + java.net.URLEncoder.encode(term, "UTF-8"))
            }
        }

        if (l.contains("joke")) return AiReply(jokes.random())
        if (Regex("(who|what) are you|your name").containsMatchIn(l)) {
            return AiReply("I'm Mat AI, a small on-device helper inside Mat Launcher. I'm rule-based, not a real language model, but I can open apps, check the weather, do math and restyle your icons.")
        }
        if (l.contains("help") || l.contains("what can you")) {
            return AiReply("Try: \"open <app>\", \"weather\", \"battery\", \"flashlight on/off\", \"transparent icons\", \"circle icons\", \"change wallpaper\", \"lock\", \"search for <thing>\", or any math like 2^10 / 4.")
        }

        // last resort: maybe they just typed an app name
        findApp(h, l)?.let { app ->
            return AiReply("Opening ${h.settings.labelFor(app.key, app.label)}…") { h.launch(app) }
        }
        return AiReply("I'm not sure about that one. Say \"help\" to see what I can do, or I can search the web for it.") {}
    }

    private fun findApp(h: AiHost, name: String): AppInfo? {
        val n = name.lowercase().removePrefix("the ").removeSuffix(" app").trim()
        if (n.isEmpty()) return null
        fun label(a: AppInfo) = h.settings.labelFor(a.key, a.label).lowercase()
        return h.apps.firstOrNull { label(it) == n }
            ?: h.apps.firstOrNull { label(it).startsWith(n) }
            ?: h.apps.firstOrNull { label(it).contains(n) }
    }
}

/** Small recursive-descent calculator: + - * / ^ % parentheses, unary minus. */
object Calc {
    fun fromText(text: String): Double? {
        var s = text.lowercase()
        s = s.replace(Regex("^(calculate|calc|what is|what's|whats|compute|solve)\\s*"), "")
        s = s.removeSuffix("?").removeSuffix("=").trim()
        s = s.replace("×", "*").replace("÷", "/").replace(Regex("(?<=\\d)\\s*x\\s*(?=[\\d(])"), "*")
        if (s.isEmpty() || !Regex("^[0-9+\\-*/().%^ ]+$").matches(s)) return null
        if (!s.any { it.isDigit() } || !s.any { it in "+-*/^%" }) return null
        return eval(s)
    }

    fun format(d: Double): String =
        if (d == Math.floor(d) && Math.abs(d) < 1e15) d.toLong().toString()
        else String.format(Locale.US, "%.6f", d).trimEnd('0').trimEnd('.')

    fun eval(src: String): Double? {
        val s = src.replace(" ", "")
        var pos = 0

        fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

        lateinit var expr: () -> Double

        fun primary(): Double {
            if (peek() == '(') {
                pos++
                val v = expr()
                if (peek() != ')') throw IllegalArgumentException("missing )")
                pos++
                return v
            }
            val start = pos
            while (peek().isDigit() || peek() == '.') pos++
            if (start == pos) throw IllegalArgumentException("number expected")
            return s.substring(start, pos).toDouble()
        }

        fun unary(): Double {
            if (peek() == '-') { pos++; return -unary() }
            if (peek() == '+') { pos++; return unary() }
            return primary()
        }

        fun power(): Double {
            val base = unary()
            if (peek() == '^') { pos++; return base.pow(power()) }
            return base
        }

        fun term(): Double {
            var v = power()
            while (true) {
                when (peek()) {
                    '*' -> { pos++; v *= power() }
                    '/' -> { pos++; v /= power() }
                    '%' -> { pos++; v %= power() }
                    else -> return v
                }
            }
        }

        expr = {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; v += term() }
                    '-' -> { pos++; v -= term() }
                    else -> break
                }
            }
            v
        }

        return try {
            val r = expr()
            if (pos != s.length || r.isNaN() || r.isInfinite()) null else r
        } catch (e: Exception) {
            null
        }
    }
}
