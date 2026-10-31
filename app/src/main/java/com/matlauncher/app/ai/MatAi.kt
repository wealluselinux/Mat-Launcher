package com.matlauncher.app.ai

import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.IconShape
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.util.RingerMode
import com.matlauncher.app.util.SystemControls
import com.matlauncher.app.weather.WeatherData
import com.matlauncher.app.weather.conditionLabel
import com.matlauncher.app.weather.tempString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

/**
 * One thing the assistant does. [text] is what it says (shown before [run] when [launch] is true,
 * i.e. when the step opens an app). [run] does the work and returns the message to show afterwards.
 */
class AiStep(
    val text: String = "",
    val launch: Boolean = false,
    val run: (suspend () -> String)? = null,
)

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
    val sys: SystemControls,
)

private class Interp(val step: AiStep, val dir: Boolean? = null)

/**
 * A tiny on-device assistant. It is NOT a language model: it's a rule-based intent matcher.
 *
 * 1. The sentence is split into clauses ("turn off wifi, then flashlight on and open youtube").
 * 2. Each clause is matched against a list of patterns (regexes + keyword checks), top to bottom.
 *    The first pattern that fits wins and becomes a step.
 * 3. A clause nobody recognises (like "dogs" in "search for cats and dogs") is glued back onto the
 *    clause before it, so splitting on "and" doesn't break searches or app names.
 * 4. Steps run in order. Opening an app always goes last, since it leaves the launcher.
 */
object MatAi {
    val suggestions = listOf(
        "Open camera", "What's the weather?", "Turn off wifi, flashlight on, open YouTube",
        "Silent mode", "Transparent icons", "12 * (4 + 3)",
    )

    private val jokes = listOf(
        "Why do Android devs love Material You? It matches their mood.",
        "I would tell you a UDP joke, but you might not get it.",
        "There are 10 kinds of people: those who understand binary and those who don't.",
        "A launcher walks into a bar. The bartender says: \"Home again?\"",
    )

    fun plan(raw: String, h: AiHost): List<AiStep> {
        val l = raw.trim().lowercase(Locale.getDefault())
        if (l.isEmpty()) return listOf(AiStep("Say something and I'll try to help!"))

        // math first so "what is 2+2" works
        Calc.fromText(l)?.let { return listOf(AiStep("= " + Calc.format(it))) }

        // 1. split into clauses, gluing unrecognised pieces back onto the clause before
        val parts = l.split(Regex("\\s*(?:,|;|\\band then\\b|\\bthen\\b|\\band also\\b|\\bafter that\\b|\\bafterwards\\b|\\bplus\\b|\\band\\b|\\balso\\b)\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val clauses = mutableListOf<String>()
        var last: Boolean? = null
        for (p in parts) {
            val i = interpret(p, h, last)
            if (i == null && clauses.isNotEmpty()) {
                clauses[clauses.lastIndex] = clauses.last() + " and " + p
            } else {
                clauses += p
                if (i?.dir != null) last = i.dir
            }
        }

        // 2. turn every clause into a step
        val steps = mutableListOf<AiStep>()
        last = null
        for (c in clauses) {
            val i = interpret(c, h, last)
            if (i == null) {
                steps += AiStep(
                    if (clauses.size == 1) "I'm not sure about that one. Say \"help\" to see what I can do."
                    else "I didn't understand \"$c\"."
                )
            } else {
                steps += i.step
                if (i.dir != null) last = i.dir
            }
        }

        // 3. opening an app leaves the launcher, so do it last, and only once
        val launches = steps.filter { it.launch }
        val rest = steps.filter { !it.launch }
        val out = mutableListOf<AiStep>()
        if (clauses.size > 1) out += AiStep("On it. ${clauses.size} steps.")
        out += rest
        if (launches.size > 1) out += AiStep("I can only open one app at a time, so I'll open the last one.")
        launches.lastOrNull()?.let { out += it }
        return out
    }

    private val onRe = Regex("\\b(on|enable|enabled|activate|start|up)\\b")
    private val offRe = Regex("\\b(off|disable|disabled|deactivate|stop|shut|kill)\\b")
    private val verbRe = Regex("\\b(turn|switch|toggle|enable|disable|activate|deactivate|set|put|go|kill|shut|start|stop|on|off)\\b")

    private fun direction(l: String): Boolean? = when {
        offRe.containsMatchIn(l) -> false
        onRe.containsMatchIn(l) -> true
        else -> null
    }

    private fun percent(l: String): Int? {
        Regex("(\\d{1,3})\\s*(%|percent)?").find(l)?.let { return it.groupValues[1].toInt().coerceIn(0, 100) }
        return when {
            Regex("\\b(max|maximum|full|highest|brightest|loudest)\\b").containsMatchIn(l) -> 100
            Regex("\\b(half|medium)\\b").containsMatchIn(l) -> 50
            Regex("\\b(min|minimum|lowest|low|dim|dimmest|quietest)\\b").containsMatchIn(l) -> 15
            else -> null
        }
    }

    /** Matches one clause against the patterns. Returns null when nothing fits. */
    private fun interpret(l: String, h: AiHost, last: Boolean?): Interp? {
        val s = h.settings
        val sys = h.sys
        val dir = direction(l)
        val hasVerb = verbRe.containsMatchIn(l)
        // "wifi off" / "bluetooth" after "turn off wifi and ...": use the direction already given
        val want = dir ?: last
        fun toggle(current: Boolean): Boolean = want ?: !current

        if (Regex("^(hi|hello|hey|yo|sup)\\b").containsMatchIn(l)) {
            return Interp(AiStep("Hey! I'm Mat AI, built into your launcher. Try \"open camera\", \"turn off wifi\" or \"silent mode\"."))
        }

        // ---- system switches (need a verb or a direction carried over from the previous clause)
        val isOpenCmd = Regex("^(open|launch|run|search|google|look up)\\b").containsMatchIn(l)
        val bareFollowUp = last != null && l.split(" ").size <= 3
        if (!isOpenCmd && (hasVerb || bareFollowUp)) {
            if (Regex("\\b(wi-?fi|wi fi|wireless)\\b").containsMatchIn(l)) {
                val on = toggle(sys.wifiOn)
                return Interp(AiStep(run = { sys.setWifi(on) }), on)
            }
            if (l.contains("bluetooth")) {
                val on = toggle(sys.bluetoothOn)
                return Interp(AiStep(run = { sys.setBluetooth(on) }), on)
            }
            if (Regex("\\b(airplane|aeroplane|flight mode)\\b").containsMatchIn(l)) {
                val on = toggle(sys.airplaneOn)
                return Interp(AiStep(run = { sys.setAirplane(on) }), on)
            }
            if (Regex("\\b(mobile data|cellular|cell data|data)\\b").containsMatchIn(l)) {
                val on = want ?: true
                return Interp(AiStep(run = { sys.setMobileData(on) }), on)
            }
            if (Regex("\\b(flashlight|torch|flash light)\\b").containsMatchIn(l)) {
                val on = toggle(sys.torchOn)
                return Interp(AiStep(run = { if (h.setTorch(on)) "Flashlight ${if (on) "on" else "off"}." else "This device doesn't seem to have a flashlight." }), on)
            }
            if (Regex("(do not disturb|don'?t disturb|dnd|focus mode)").containsMatchIn(l)) {
                val on = toggle(sys.dndOn)
                return Interp(AiStep(run = { sys.setDnd(on) }), on)
            }
            if (Regex("\\b(auto.?rotate|rotation|rotate)\\b").containsMatchIn(l)) {
                val on = toggle(sys.rotationAuto)
                return Interp(AiStep(run = { sys.setRotation(on) }), on)
            }
        }
        // sound modes
        if (!isOpenCmd && !l.contains("volume") && !l.contains("brightness")) {
            val silentWord = Regex("\\b(silent|silence|mute|vibrate|vibration)\\b").containsMatchIn(l)
            val m = when {
                l.contains("unmute") || Regex("\\b(normal|ring|ringer|loud) mode\\b").containsMatchIn(l) || (silentWord && dir == false) -> RingerMode.NORMAL
                Regex("\\b(vibrate|vibration)\\b").containsMatchIn(l) -> RingerMode.VIBRATE
                Regex("\\b(silent|silence|mute)\\b").containsMatchIn(l) -> RingerMode.SILENT
                else -> null
            }
            if (m != null) return Interp(AiStep(run = { sys.setRinger(m) }))
        }
        if (l.contains("brightness") || Regex("\\b(brighter|dimmer)\\b").containsMatchIn(l)) {
            val pct = percent(l) ?: when {
                l.contains("brighter") || Regex("\\b(up|raise)\\b").containsMatchIn(l) -> (sys.brightness * 100).toInt() + 20
                l.contains("dimmer") || Regex("\\b(down|lower)\\b").containsMatchIn(l) -> (sys.brightness * 100).toInt() - 20
                else -> null
            }
            return if (pct == null) Interp(AiStep("Tell me a level, like \"brightness 60%\"."))
            else Interp(AiStep(run = { sys.setBrightnessPercent(pct.coerceIn(1, 100)) }))
        }
        if (l.contains("volume")) {
            val pct = percent(l) ?: when {
                Regex("\\b(up|louder|raise)\\b").containsMatchIn(l) -> (sys.volume * 100).toInt() + 20
                Regex("\\b(down|quieter|lower)\\b").containsMatchIn(l) -> (sys.volume * 100).toInt() - 20
                else -> null
            }
            return if (pct == null) Interp(AiStep("Tell me a level, like \"volume 40%\"."))
            else Interp(AiStep(run = { sys.setVolumePercent(pct.coerceIn(0, 100)) }))
        }

        // ---- apps
        Regex("^(open|launch|start|run)\\s+(.+)$").find(l)?.let { m ->
            val name = m.groupValues[2].trim()
            val app = findApp(h, name)
            return if (app != null) {
                Interp(AiStep("Opening ${s.labelFor(app.key, app.label)}…", launch = true, run = { h.launch(app); "" }))
            } else {
                Interp(AiStep("I couldn't find an app called \"$name\"."))
            }
        }

        // ---- information
        if (Regex("\\b(weather|temperature|forecast|rain|how (hot|cold))\\b").containsMatchIn(l)) {
            val w = h.weather ?: return Interp(AiStep("The weather hasn't loaded yet. Check your connection and try again."))
            val f = s.useFahrenheit
            return Interp(
                AiStep(
                    "${s.cityName}: ${tempString(w.tempC, f)} and ${conditionLabel(w.code).lowercase()}. " +
                        "Feels like ${tempString(w.feelsC, f)}, humidity ${w.humidity}%."
                )
            )
        }
        if (Regex("\\b(time|clock)\\b").containsMatchIn(l)) {
            return Interp(AiStep("It's " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."))
        }
        if (Regex("\\b(date|day is it|today)\\b").containsMatchIn(l)) {
            return Interp(AiStep("Today is " + SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) + "."))
        }
        if (l.contains("battery")) return Interp(AiStep("Your battery is at ${h.battery()}%."))

        if (l.contains("wallpaper") || l.contains("background")) {
            return Interp(AiStep("Opening wallpapers…", run = { h.openWallpaper(); "" }))
        }

        // ---- icon / launcher styling
        if (l.contains("icon") || l.contains("label")) {
            when {
                l.contains("transparent") || l.contains("no background") -> return styled("Done: icons are transparent now.") { s.iconStyle = IconStyle.TRANSPARENT }
                l.contains("match") || l.contains("theme") || l.contains("wallpaper") || l.contains("material") ->
                    return styled("Done: icons now match your wallpaper colors.") { s.iconStyle = IconStyle.THEMED }
                l.contains("glass") -> return styled("Done: glass icons.") { s.iconStyle = IconStyle.GLASS }
                l.contains("original") || l.contains("default") -> return styled("Done: original icon colors.") { s.iconStyle = IconStyle.ORIGINAL }
                l.contains("circle") -> return styled("Circle icons it is.") { s.iconShape = IconShape.CIRCLE }
                l.contains("squircle") -> return styled("Squircle icons it is.") { s.iconShape = IconShape.SQUIRCLE }
                l.contains("teardrop") -> return styled("Teardrop icons it is.") { s.iconShape = IconShape.TEARDROP }
                l.contains("round") -> return styled("Rounded-square icons it is.") { s.iconShape = IconShape.ROUNDED }
                l.contains("hide") && l.contains("label") -> return styled("Labels hidden.") { s.showLabels = false }
                l.contains("show") && l.contains("label") -> return styled("Labels shown.") { s.showLabels = true }
            }
        }

        if (l == "lock" || l.contains("lock screen") || l.contains("lock my") || l.contains("lock the") || l == "lock it") {
            return Interp(AiStep("Locking…", run = { h.lockNow(); "" }))
        }
        if (l.contains("setting")) return Interp(AiStep("Opening Mat Launcher settings…", run = { h.openSettings(); "" }))

        Regex("^(search|google|look up)( for)?\\s+(.+)$").find(l)?.let { m ->
            val term = m.groupValues[3]
            return Interp(
                AiStep("Searching the web for \"$term\"…", run = {
                    h.openUrl("https://www.google.com/search?q=" + java.net.URLEncoder.encode(term, "UTF-8"))
                    ""
                })
            )
        }

        if (l.contains("joke")) return Interp(AiStep(jokes.random()))
        if (Regex("how (do|does) (you|this|it) work|how are you (made|built)|are you (real|an ai|chatgpt|gpt)").containsMatchIn(l)) {
            return Interp(
                AiStep(
                    "I'm not a language model. I split what you type into steps, match each step against a list of patterns, " +
                        "and run the first one that fits. Everything happens on your phone, nothing is sent anywhere."
                )
            )
        }
        if (Regex("(who|what) are you|your name").containsMatchIn(l)) {
            return Interp(AiStep("I'm Mat AI, a small on-device helper inside Mat Launcher. I'm rule-based, not a real language model, but I can chain commands: open apps, switch things on and off, check the weather, do math and restyle your icons."))
        }
        if (l.contains("help") || l.contains("what can you")) {
            return Interp(
                AiStep(
                    "Try one or several at once, like \"turn off wifi, flashlight on, open YouTube\":\n" +
                        "• open <app>\n• wifi / bluetooth / airplane mode / mobile data on or off (needs Shizuku)\n" +
                        "• flashlight, do not disturb, auto-rotate on or off\n• silent / vibrate / normal mode\n" +
                        "• brightness 60%, volume 30%\n• weather, battery, time\n• transparent icons, circle icons\n" +
                        "• change wallpaper, lock, search for <thing>, or math like 2^10 / 4."
                )
            )
        }

        // last resort: maybe they just typed an app name
        findApp(h, l)?.let { app ->
            return Interp(AiStep("Opening ${s.labelFor(app.key, app.label)}…", launch = true, run = { h.launch(app); "" }))
        }
        return null
    }

    private fun styled(message: String, change: () -> Unit) = Interp(AiStep(run = { change(); message }))

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
