package com.matlauncher.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matlauncher.app.data.ClockStyle
import com.matlauncher.app.data.HAlign
import com.matlauncher.app.data.LauncherSettings
import com.matlauncher.app.data.TextScale
import com.matlauncher.app.data.PhotoShape
import com.matlauncher.app.data.WeatherStyle
import com.matlauncher.app.util.ImageTools
import com.matlauncher.app.weather.WeatherData
import com.matlauncher.app.weather.conditionLabel
import com.matlauncher.app.weather.tempString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1000L - (value % 1000L))
    }
}

/** The scalloped "cookie" shape Material You widgets use. */
val CookieShape = GenericShape { size, _ ->
    val bumps = 9
    val amp = 0.055f
    val steps = 180
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(size.width, size.height) / 2f / (1f + amp)
    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val rr = r * (1f + amp * cos(bumps * t).toFloat())
        val x = cx + rr * cos(t).toFloat()
        val y = cy + rr * sin(t).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ------------------------------------------------------------ at a glance

@Composable
fun AtAGlance(weather: WeatherData?, settings: LauncherSettings, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val date = SimpleDateFormat(settings.topBarDateStyle.pattern, Locale.getDefault()).format(Date(now))
    val centered = settings.topBarAlign == HAlign.CENTER
    val titleStyle = when (settings.topBarSize) {
        TextScale.SMALL -> MaterialTheme.typography.titleLarge
        TextScale.MEDIUM -> MaterialTheme.typography.headlineMedium
        TextScale.LARGE -> MaterialTheme.typography.displaySmall
    }
    val subStyle = when (settings.topBarSize) {
        TextScale.SMALL -> MaterialTheme.typography.bodyMedium
        TextScale.MEDIUM -> MaterialTheme.typography.titleMedium
        TextScale.LARGE -> MaterialTheme.typography.titleLarge
    }
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (settings.topBarDate) {
            Text(date, color = Color.White, style = titleStyle.merge(OnWallpaper), textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        }
        if (settings.topBarWeather && weather != null) {
            Text(
                tempString(weather.tempC, settings.useFahrenheit) + "  ·  " + conditionLabel(weather.code),
                color = Color.White.copy(alpha = 0.92f),
                style = subStyle.merge(OnWallpaper),
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

// ------------------------------------------------------------------ clock

@Composable
fun ClockWidget(settings: LauncherSettings, modifier: Modifier = Modifier, compact: Boolean = false) {
    val now by rememberNow()
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val is24 = DateFormat.is24HourFormat(ctx)
    val hh = SimpleDateFormat(if (is24) "HH" else "hh", Locale.getDefault()).format(Date(now))
    val mm = SimpleDateFormat("mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(now))

    when (settings.clockStyle) {
        ClockStyle.MINIMAL -> {
            Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
                Text(
                    "$hh:$mm",
                    color = Color.White,
                    fontSize = if (compact) 48.sp else 72.sp,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.displayLarge.merge(OnWallpaper),
                )
                if (!compact) Text(date, color = Color.White, style = MaterialTheme.typography.titleMedium.merge(OnWallpaper))
            }
        }
        else -> Surface(
            modifier = modifier,
            shape = RoundedCornerShape(40.dp),
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
        ) {
            when (settings.clockStyle) {
                ClockStyle.ANALOG -> Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxSize().clip(CookieShape).background(scheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnalogClock(now, Modifier.fillMaxSize().padding(16.dp))
                    }
                }
                ClockStyle.DIGITAL -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "$hh:$mm",
                        fontSize = if (compact) 38.sp else 68.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!compact) Text(date, style = MaterialTheme.typography.titleMedium)
                }
                else -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hh, fontSize = 72.sp, lineHeight = 64.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(
                        mm,
                        fontSize = 72.sp,
                        lineHeight = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = scheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Natural height of the clock when it is laid out full width. */
fun clockHeight(style: ClockStyle) = when (style) {
    ClockStyle.STACKED -> 160.dp
    ClockStyle.DIGITAL -> 130.dp
    ClockStyle.ANALOG -> 210.dp
    ClockStyle.MINIMAL -> 110.dp
}

@Composable
fun AnalogClock(nowMillis: Long, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val tick = scheme.onSecondaryContainer
    val hand = scheme.primary
    val second = scheme.tertiary
    Canvas(modifier) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val h = (cal.get(Calendar.HOUR) % 12) + cal.get(Calendar.MINUTE) / 60f
        val m = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        val s = cal.get(Calendar.SECOND).toFloat()
        val r = size.minDimension / 2f
        val c = center
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                color = tick,
                start = c + dir * (r * 0.84f),
                end = c + dir * (r * 0.95f),
                strokeWidth = if (i % 3 == 0) 6f else 3f,
                cap = StrokeCap.Round,
            )
        }
        fun drawHand(angleDeg: Float, length: Float, width: Float, color: Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                color = color,
                start = c,
                end = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * length),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        drawHand(h * 30f, 0.5f, 10f, hand)
        drawHand(m * 6f, 0.75f, 7f, hand)
        drawHand(s * 6f, 0.82f, 3f, second)
        drawCircle(color = hand, radius = 8f, center = c)
    }
}

// ---------------------------------------------------------------- weather

fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2, 3 -> Icons.Rounded.WbCloudy
    45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.FlashOn
    else -> Icons.Rounded.WbCloudy
}

fun weatherHeight(style: WeatherStyle) = when (style) {
    WeatherStyle.CARD -> 172.dp
    WeatherStyle.PILL -> 84.dp
    WeatherStyle.MINIMAL -> 60.dp
}

/** Weather widget in three looks: Pixel-style card, pill, or minimal text. */
@Composable
fun WeatherWidget(
    weather: WeatherData?,
    settings: LauncherSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val f = settings.useFahrenheit

    if (settings.weatherStyle == WeatherStyle.MINIMAL) {
        Row(
            modifier.clickable { onClick() }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (weather == null) {
                Text("Loading weather…", color = Color.White, style = MaterialTheme.typography.titleMedium.merge(OnWallpaper))
            } else {
                Icon(weatherIcon(weather.code, weather.isDay), null, Modifier.size(34.dp), tint = Color.White)
                Text(
                    tempString(weather.tempC, f),
                    color = Color.White,
                    fontSize = 34.sp,
                    style = MaterialTheme.typography.headlineMedium.merge(OnWallpaper),
                )
                Text(
                    conditionLabel(weather.code) + " · " + settings.cityName,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleSmall.merge(OnWallpaper),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = if (settings.weatherStyle == WeatherStyle.PILL) RoundedCornerShape(50) else RoundedCornerShape(36.dp),
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
    ) {
        if (weather == null) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                Text("Loading weather for ${settings.cityName}…", style = MaterialTheme.typography.titleSmall)
            }
        } else if (settings.weatherStyle == WeatherStyle.PILL) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(52.dp).clip(CookieShape).background(scheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(weatherIcon(weather.code, weather.isDay), null, Modifier.size(28.dp), tint = scheme.onTertiaryContainer)
                }
                Text(tempString(weather.tempC, f), fontSize = 36.sp, fontWeight = FontWeight.Medium)
                Column(Modifier.weight(1f)) {
                    Text(settings.cityName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(conditionLabel(weather.code), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        } else {
            val wind = if (f) "${(weather.windKmh * 0.621371).toInt()} mph" else "${weather.windKmh.toInt()} km/h"
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(settings.cityName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tempString(weather.tempC, f), fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal)
                    }
                    Box(
                        Modifier.size(52.dp).clip(CookieShape).background(scheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            weatherIcon(weather.code, weather.isDay),
                            conditionLabel(weather.code),
                            Modifier.size(28.dp),
                            tint = scheme.onTertiaryContainer,
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                Text("Precipitation: ${weather.precipPct}%", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text("Humidity: ${weather.humidity}%", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text("Wind: $wind", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------------ photo

@Composable
fun PhotoWidget(settings: LauncherSettings, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val photos = settings.photos
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(photos.size) {
        while (photos.size > 1) {
            delay(8000)
            index = (index + 1) % photos.size
        }
    }
    val file = if (photos.isEmpty()) null else photos[index % photos.size]
    val bmp: ImageBitmap? by produceState<ImageBitmap?>(null, file) {
        value = file?.let { f ->
            withContext(Dispatchers.IO) { ImageTools.decodeFile(f, 1280)?.asImageBitmap() }
        }
    }

    val shape: androidx.compose.ui.graphics.Shape = when (settings.photoShape) {
        PhotoShape.ROUNDED -> RoundedCornerShape(44.dp)
        PhotoShape.COOKIE -> CookieShape
        PhotoShape.CIRCLE -> CircleShape
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.secondaryContainer,
    ) {
        Box(
            Modifier.fillMaxSize().clickable(enabled = photos.size > 1) { index = (index + 1) % photos.size }
        ) {
            val b = bmp
            if (b != null) {
                Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))
                    )
                )
            } else {
                Column(
                    Modifier.fillMaxSize().clickable { onAdd() },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(84.dp).clip(CookieShape).background(scheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(36.dp), tint = scheme.onTertiaryContainer)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Add photos to your widget",
                        color = scheme.onSecondaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (file != null) {
                Row(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    IconButton(
                        onClick = { settings.removePhoto(file); index = 0 },
                        modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                    ) { Icon(Icons.Rounded.Delete, "Remove photo", tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                    ) { Icon(Icons.Rounded.AddPhotoAlternate, "Add photos", tint = Color.White) }
                }
            }
        }
    }
}
