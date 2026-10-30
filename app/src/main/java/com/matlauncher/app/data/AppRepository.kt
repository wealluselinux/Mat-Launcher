package com.matlauncher.app.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val key: String,
    val pkg: String,
    val cls: String,
    val label: String,
    val icon: Drawable,
)

class AppRepository(private val context: Context) {
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    suspend fun load() {
        val result = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { ri ->
                    AppInfo(
                        key = ri.activityInfo.packageName + "/" + ri.activityInfo.name,
                        pkg = ri.activityInfo.packageName,
                        cls = ri.activityInfo.name,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
        apps = result
    }

    fun byKey(key: String): AppInfo? = apps.firstOrNull { it.key == key }

    /** Picks sensible default dock apps (phone, messages, browser, camera). */
    fun defaultDockKeys(): List<String> {
        val pm = context.packageManager
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")),
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
        )
        val keys = ArrayList<String>()
        for (p in probes) {
            val pkg = pm.resolveActivity(p, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName ?: continue
            val app = apps.firstOrNull { it.pkg == pkg } ?: continue
            if (app.key !in keys) keys.add(app.key)
        }
        return keys
    }
}
