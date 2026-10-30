package com.matlauncher.app.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperPreset(val name: String, val colors: IntArray)

object WallpaperTools {
    val presets = listOf(
        WallpaperPreset("Lavender", intArrayOf(0xFF2A1B5C.toInt(), 0xFF6750A4.toInt(), 0xFFD0BCFF.toInt())),
        WallpaperPreset("Ocean", intArrayOf(0xFF00262D.toInt(), 0xFF006874.toInt(), 0xFF4FD8EB.toInt())),
        WallpaperPreset("Meadow", intArrayOf(0xFF0B2E15.toInt(), 0xFF386A20.toInt(), 0xFFB7F397.toInt())),
        WallpaperPreset("Sunset", intArrayOf(0xFF3E1038.toInt(), 0xFFC2185B.toInt(), 0xFFFFB74D.toInt())),
        WallpaperPreset("Peach", intArrayOf(0xFF4A1F10.toInt(), 0xFFE57A44.toInt(), 0xFFFFDBCA.toInt())),
        WallpaperPreset("Midnight", intArrayOf(0xFF05070D.toInt(), 0xFF1B2A4A.toInt(), 0xFF5C7CBA.toInt())),
    )

    fun render(p: WallpaperPreset, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, w * 0.6f, h.toFloat(), p.colors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        fun glow(cx: Float, cy: Float, r: Float, color: Int) {
            val g = Paint(Paint.ANTI_ALIAS_FLAG)
            val transparent = color and 0x00FFFFFF
            g.shader = RadialGradient(cx, cy, r, intArrayOf((0x66 shl 24) or transparent, transparent), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, g)
        }
        glow(w * 0.85f, h * 0.18f, w * 0.7f, p.colors[2])
        glow(w * 0.1f, h * 0.62f, w * 0.8f, p.colors[1])
        glow(w * 0.7f, h * 0.95f, w * 0.6f, p.colors[2])
        return bmp
    }

    suspend fun setPreset(ctx: Context, p: WallpaperPreset): Boolean = withContext(Dispatchers.IO) {
        try {
            val dm = ctx.resources.displayMetrics
            val bmp = render(p, dm.widthPixels, dm.heightPixels)
            WallpaperManager.getInstance(ctx)
                .setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setFromUri(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                WallpaperManager.getInstance(ctx)
                    .setStream(input, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
