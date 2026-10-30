package com.matlauncher.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.matlauncher.app.data.AppInfo
import com.matlauncher.app.data.IconShape
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Path as AndroidPath

/**
 * Draws app icons in the user's chosen shape + style:
 *  - Original: the app's own icon, clipped to the shape
 *  - Themed: Material You plate + monochrome glyph tinted from the wallpaper-derived color scheme
 *  - Transparent: just the glyph, no plate
 *  - Glass: frosted translucent plate + glyph
 */
object IconRenderer {
    private val cache = LruCache<String, ImageBitmap>(400)

    fun render(
        app: AppInfo,
        px: Int,
        shape: IconShape,
        style: IconStyle,
        plate: Int,
        glyph: Int,
        custom: File?,
        version: Int,
    ): ImageBitmap {
        val key = "${app.key}|$px|${shape.name}|${style.name}|$plate|$glyph|${custom != null}|$version"
        cache.get(key)?.let { return it }
        val img = draw(app, px, shape, style, plate, glyph, custom).asImageBitmap()
        cache.put(key, img)
        return img
    }

    private fun shapePath(shape: IconShape, s: Float): AndroidPath {
        val p = AndroidPath()
        when (shape) {
            IconShape.CIRCLE -> p.addCircle(s / 2, s / 2, s / 2, AndroidPath.Direction.CW)
            IconShape.SQUIRCLE -> {
                val n = 4.0
                val steps = 90
                for (i in 0..steps) {
                    val t = 2.0 * PI * i / steps
                    val cx = cos(t)
                    val sy = sin(t)
                    val x = s / 2 + (s / 2) * (sign(cx) * abs(cx).pow(2.0 / n)).toFloat()
                    val y = s / 2 + (s / 2) * (sign(sy) * abs(sy).pow(2.0 / n)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }
            IconShape.ROUNDED -> p.addRoundRect(RectF(0f, 0f, s, s), s * 0.3f, s * 0.3f, AndroidPath.Direction.CW)
            IconShape.TEARDROP -> {
                val big = s / 2
                val small = s * 0.12f
                p.addRoundRect(
                    RectF(0f, 0f, s, s),
                    floatArrayOf(big, big, big, big, big, big, small, small),
                    AndroidPath.Direction.CW
                )
            }
            IconShape.NONE -> p.addRect(0f, 0f, s, s, AndroidPath.Direction.CW)
        }
        return p
    }

    private fun clone(d: Drawable): Drawable = d.constantState?.newDrawable()?.mutate() ?: d

    /** Bounds that make an adaptive layer's 72dp "visible area" fill exactly [size]. */
    private fun layerBounds(size: Float, zoom: Float = 1f): IntArray {
        val full = size * 1.5f * zoom
        val off = ((size - full) / 2f)
        return intArrayOf(off.toInt(), off.toInt(), (off + full).toInt(), (off + full).toInt())
    }

    private fun drawLayer(c: AndroidCanvas, d: Drawable, size: Float, zoom: Float = 1f) {
        val b = layerBounds(size, zoom)
        d.setBounds(b[0], b[1], b[2], b[3])
        d.draw(c)
    }

    private fun drawInset(c: AndroidCanvas, d: Drawable, size: Float, inset: Float) {
        val i = (size * inset).toInt()
        d.setBounds(i, i, (size - i).toInt(), (size - i).toInt())
        d.draw(c)
    }

    /** Draws just the glyph of an icon (adaptive foreground, or the whole legacy icon) with a soft shadow. */
    private fun drawGlyphWithShadow(c: AndroidCanvas, d: Drawable, px: Int, zoom: Float) {
        val tmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val tc = AndroidCanvas(tmp)
        val fg = (d as? AdaptiveIconDrawable)?.foreground
        if (fg != null) drawLayer(tc, fg, px.toFloat(), zoom) else drawInset(tc, d, px.toFloat(), 0.1f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.setShadowLayer(px * 0.035f, 0f, px * 0.02f, 0x59000000)
        c.drawBitmap(tmp, 0f, 0f, paint)
    }

    private fun draw(
        app: AppInfo,
        px: Int,
        shape: IconShape,
        style: IconStyle,
        plate: Int,
        glyph: Int,
        customFile: File?,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = AndroidCanvas(bmp)
        val size = px.toFloat()
        val path = shapePath(shape, size)
        val hasPlate = shape != IconShape.NONE
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // user-picked image wins over everything
        if (customFile != null) {
            val src = BitmapFactory.decodeFile(customFile.path)
            if (src != null) {
                c.save()
                if (hasPlate) c.clipPath(path)
                c.drawBitmap(src, null, RectF(0f, 0f, size, size), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                c.restore()
                return bmp
            }
        }

        val d = clone(app.icon)
        val adaptive = d as? AdaptiveIconDrawable

        fun originalClipped() {
            c.save()
            if (hasPlate) c.clipPath(path)
            if (adaptive != null) {
                drawLayer(c, adaptive.background, size)
                drawLayer(c, adaptive.foreground, size)
            } else {
                if (hasPlate) {
                    fill.color = plate
                    c.drawPath(path, fill)
                    drawInset(c, d, size, 0.14f)
                } else {
                    drawInset(c, d, size, 0.04f)
                }
            }
            c.restore()
        }

        when (style) {
            IconStyle.ORIGINAL -> originalClipped()

            IconStyle.THEMED -> {
                val mono: Drawable? = if (Build.VERSION.SDK_INT >= 33) adaptive?.monochrome else null
                if (mono != null) {
                    val m = clone(mono)
                    m.setTint(glyph)
                    if (hasPlate) {
                        fill.color = plate
                        c.drawPath(path, fill)
                    }
                    c.save()
                    if (hasPlate) c.clipPath(path)
                    drawLayer(c, m, size)
                    c.restore()
                } else if (adaptive != null) {
                    // no monochrome layer: tint the foreground silhouette instead
                    val fg = adaptive.foreground
                    fg.colorFilter = PorterDuffColorFilter(glyph, PorterDuff.Mode.SRC_IN)
                    if (hasPlate) {
                        fill.color = plate
                        c.drawPath(path, fill)
                    }
                    c.save()
                    if (hasPlate) c.clipPath(path)
                    drawLayer(c, fg, size)
                    c.restore()
                } else {
                    originalClipped()
                }
            }

            IconStyle.TRANSPARENT -> drawGlyphWithShadow(c, d, px, 0.9f)

            IconStyle.GLASS -> {
                if (hasPlate) {
                    fill.color = (plate and 0x00FFFFFF) or (0x70 shl 24)
                    c.drawPath(path, fill)
                    val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
                    stroke.style = Paint.Style.STROKE
                    stroke.strokeWidth = size * 0.012f
                    stroke.color = 0x66FFFFFF
                    c.drawPath(path, stroke)
                }
                drawGlyphWithShadow(c, d, px, 0.85f)
            }
        }
        return bmp
    }
}

@Composable
fun AppIcon(app: AppInfo, settings: LauncherSettings, size: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val drawSize = size * settings.iconScale
    val px = with(density) { drawSize.roundToPx() }.coerceAtLeast(8)
    val scheme = MaterialTheme.colorScheme
    val plate = scheme.secondaryContainer.toArgb()
    val glyph = scheme.onSecondaryContainer.toArgb()
    val version = settings.iconVersion
    val shape = settings.iconShape
    val style = settings.iconStyle
    val custom = remember(version, app.key) { settings.customIconFile(app.key).takeIf { it.exists() } }
    val img = remember(app.key, px, shape, style, plate, glyph, version, custom) {
        IconRenderer.render(app, px, shape, style, plate, glyph, custom, version)
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(bitmap = img, contentDescription = app.label, modifier = Modifier.size(drawSize))
    }
}
