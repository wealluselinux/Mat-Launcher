package com.matlauncher.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import rikka.shizuku.Shizuku

/**
 * Android doesn't let normal apps switch Wi-Fi, Bluetooth, airplane mode or mobile data. The only
 * no-root way is Shizuku: a separate app that you start once (over wireless debugging) which lends
 * this app shell-level access. Everything here is optional; without Shizuku the toggles just open
 * the system panels.
 */
object ShellAccess {
    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 4101

    fun installed(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
        true
    } catch (e: Exception) {
        false
    }

    /** Shizuku's service is up (the app is installed and has been started). */
    fun running(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    /** Running and this app has been allowed to use it. */
    fun granted(): Boolean = try {
        running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Throwable) {
            // not running
        }
    }

    fun openShizuku(ctx: Context) {
        val i = ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (i != null) ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openDownloadPage(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // no browser
        }
    }

    private val newProcess by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
    }

    /** Runs a shell command with Shizuku's privileges. Blocking: call from a background thread. */
    fun run(cmd: List<String>): Boolean = try {
        val p = newProcess.invoke(null, cmd.toTypedArray(), null, null) as Process
        p.waitFor() == 0
    } catch (e: Throwable) {
        false
    }
}
