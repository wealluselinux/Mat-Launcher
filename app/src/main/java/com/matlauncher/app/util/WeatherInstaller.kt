package com.matlauncher.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android never lets one APK silently install another, so the Mat Weather APK is bundled
 * inside the launcher and handed to the system installer (the user taps "Install").
 */
object WeatherInstaller {
    const val PKG = "com.matlauncher.weather"
    private const val ASSET = "mat-weather.apk"

    fun isInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(PKG, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun isBundled(ctx: Context): Boolean = try {
        ctx.assets.open(ASSET).close()
        true
    } catch (e: Exception) {
        false
    }

    fun install(ctx: Context) {
        if (!isBundled(ctx)) {
            Toast.makeText(ctx, "This build doesn't include the Mat Weather APK", Toast.LENGTH_LONG).show()
            return
        }
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(ctx, "Allow installs from Mat Launcher, then tap Install again", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        try {
            val dir = File(ctx.cacheDir, "apk").also { it.mkdirs() }
            val out = File(dir, ASSET)
            ctx.assets.open(ASSET).use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", out)
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(ctx, "Couldn't start the installer", Toast.LENGTH_LONG).show()
        }
    }
}
