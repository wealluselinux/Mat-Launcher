package com.matlauncher.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ReleaseInfo(
    val tag: String,
    val version: String,
    val notes: String,
    val launcherUrl: String?,
    val launcherSize: Long,
    val weatherUrl: String?,
    val weatherSize: Long,
    val needLauncher: Boolean,
    val needWeather: Boolean,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val label: String, val progress: Float) : UpdateState
    data class Ready(val info: ReleaseInfo, val launcherFile: File?, val weatherFile: File?) : UpdateState
    data class Error(val message: String) : UpdateState
}

/**
 * Self-updater. Reads the latest GitHub release of Mat Launcher, downloads MatLauncher.apk (and
 * MatWeather.apk when Mat Weather is installed) and hands them to the system installer. Android
 * always asks the user to confirm an install, so this can't be fully silent.
 */
class Updater {
    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    private val api = "https://api.github.com/repos/wealluselinux/Mat-Launcher/releases/latest"

    fun installedLauncherVersion(ctx: Context): String = versionOf(ctx, ctx.packageName) ?: "0"

    fun installedWeatherVersion(ctx: Context): String? = versionOf(ctx, WeatherInstaller.PKG)

    private fun versionOf(ctx: Context, pkg: String): String? = try {
        ctx.packageManager.getPackageInfo(pkg, 0).versionName
    } catch (e: Exception) {
        null
    }

    suspend fun check(ctx: Context) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        state = try {
            val json = withContext(Dispatchers.IO) { httpText(api) }
            val o = JSONObject(json)
            val tag = o.getString("tag_name")
            val version = tag.trimStart('v', 'V')
            var launcherUrl: String? = null
            var launcherSize = 0L
            var weatherUrl: String? = null
            var weatherSize = 0L
            val assets = o.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name").lowercase()
                    if (!name.endsWith(".apk")) continue
                    val url = a.optString("browser_download_url")
                    val size = a.optLong("size", 0L)
                    if (name.contains("weather")) {
                        weatherUrl = url; weatherSize = size
                    } else {
                        launcherUrl = url; launcherSize = size
                    }
                }
            }
            val cur = installedLauncherVersion(ctx)
            val needLauncher = launcherUrl != null && isNewer(version, cur)
            val weatherCur = installedWeatherVersion(ctx)
            val needWeather = weatherUrl != null && weatherCur != null && isNewer(version, weatherCur)
            if (needLauncher || needWeather) {
                UpdateState.Available(
                    ReleaseInfo(
                        tag, version, o.optString("body").trim(),
                        launcherUrl, launcherSize, weatherUrl, weatherSize,
                        needLauncher, needWeather,
                    )
                )
            } else if (launcherUrl == null && isNewer(version, cur)) {
                UpdateState.Error("Release $tag has no MatLauncher.apk attached yet")
            } else {
                UpdateState.UpToDate(cur)
            }
        } catch (e: java.io.FileNotFoundException) {
            UpdateState.Error("No releases published yet")
        } catch (e: Exception) {
            UpdateState.Error("Couldn't check for updates (${e.message ?: "network error"})")
        }
    }

    suspend fun download(ctx: Context) {
        val info = (state as? UpdateState.Available)?.info ?: return
        val dir = File(ctx.cacheDir, "apk").also { it.mkdirs() }
        try {
            var launcherFile: File? = null
            var weatherFile: File? = null
            val total = (if (info.needWeather) info.weatherSize else 0L) + (if (info.needLauncher) info.launcherSize else 0L)
            var done = 0L
            suspend fun fetch(url: String, out: File, label: String, expectedPkg: String) {
                withContext(Dispatchers.IO) {
                    val c = open(url)
                    c.inputStream.use { input ->
                        out.outputStream().use { o ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                o.write(buf, 0, n)
                                done += n
                                if (total > 0) state = UpdateState.Downloading(label, (done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                    val pi = ctx.packageManager.getPackageArchiveInfo(out.absolutePath, 0)
                    if (pi == null || pi.packageName != expectedPkg) {
                        out.delete()
                        throw IllegalStateException("Downloaded file isn't the $label APK")
                    }
                }
            }
            if (info.needWeather && info.weatherUrl != null) {
                state = UpdateState.Downloading("Mat Weather", 0f)
                weatherFile = File(dir, "MatWeather-update.apk")
                fetch(info.weatherUrl, weatherFile, "Mat Weather", WeatherInstaller.PKG)
            }
            if (info.needLauncher && info.launcherUrl != null) {
                state = UpdateState.Downloading("Mat Launcher", if (total > 0) done.toFloat() / total else 0f)
                launcherFile = File(dir, "MatLauncher-update.apk")
                fetch(info.launcherUrl, launcherFile, "Mat Launcher", ctx.packageName)
            }
            state = UpdateState.Ready(info, launcherFile, weatherFile)
        } catch (e: Exception) {
            state = UpdateState.Error("Download failed (${e.message ?: "network error"})")
        }
    }

    /** True once the installed Mat Weather matches the downloaded release. */
    fun weatherDone(ctx: Context, info: ReleaseInfo): Boolean {
        val cur = installedWeatherVersion(ctx) ?: return false
        return !isNewer(info.version, cur)
    }

    fun install(ctx: Context, file: File) {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(ctx, "Allow installs from Mat Launcher, then tap Install again", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        try {
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(ctx, "Couldn't start the installer", Toast.LENGTH_LONG).show()
        }
    }

    fun reset() { state = UpdateState.Idle }

    // ------------------------------------------------------------------ http

    private fun open(url: String): HttpURLConnection {
        var u = url
        repeat(5) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "MatLauncher-Updater")
            c.setRequestProperty("Accept", "application/vnd.github+json")
            val code = c.responseCode
            if (code in 300..399) {
                val next = c.getHeaderField("Location") ?: throw java.io.IOException("Bad redirect")
                c.disconnect()
                u = next
            } else if (code == 404) {
                throw java.io.FileNotFoundException(url)
            } else if (code !in 200..299) {
                throw java.io.IOException("HTTP $code")
            } else {
                return c
            }
        }
        throw java.io.IOException("Too many redirects")
    }

    private fun httpText(url: String): String = open(url).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }

    companion object {
        /** True when [a] is a higher version than [b] (numeric dotted compare, ignores a leading v). */
        fun isNewer(a: String, b: String): Boolean {
            fun parts(v: String) = v.trimStart('v', 'V').split(".", "-", "+").map { it.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val x = parts(a)
            val y = parts(b)
            for (i in 0 until maxOf(x.size, y.size)) {
                val p = x.getOrElse(i) { 0 }
                val q = y.getOrElse(i) { 0 }
                if (p != q) return p > q
            }
            return false
        }
    }
}
