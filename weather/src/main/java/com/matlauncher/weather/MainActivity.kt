package com.matlauncher.weather

import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val cityState = mutableStateOf("New York")
    private val latState = mutableStateOf(40.7128)
    private val lonState = mutableStateOf(-74.006)
    private val fahrenheitState = mutableStateOf(false)

    private val prefs by lazy { getSharedPreferences("mat_weather", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AColor.TRANSPARENT, AColor.TRANSPARENT),
        )
        cityState.value = prefs.getString("city", "New York") ?: "New York"
        latState.value = prefs.getFloat("lat", 40.7128f).toDouble()
        lonState.value = prefs.getFloat("lon", -74.006f).toDouble()
        fahrenheitState.value = prefs.getBoolean("f", java.util.Locale.getDefault().country == "US")
        applyIntent(intent)

        setContent {
            WeatherTheme {
                WeatherScreen(
                    city = cityState.value,
                    lat = latState.value,
                    lon = lonState.value,
                    fahrenheit = fahrenheitState.value,
                    onCity = { name, lat, lon -> setCity(name, lat, lon) },
                    onUnit = { f -> fahrenheitState.value = f; prefs.edit().putBoolean("f", f).apply() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
    }

    /** Mat Launcher passes its selected city along when it opens this app. */
    private fun applyIntent(i: Intent?) {
        if (i == null || !i.hasExtra("lat")) return
        val name = i.getStringExtra("city") ?: return
        setCity(name, i.getDoubleExtra("lat", latState.value), i.getDoubleExtra("lon", lonState.value))
        if (i.hasExtra("fahrenheit")) {
            val f = i.getBooleanExtra("fahrenheit", false)
            fahrenheitState.value = f
            prefs.edit().putBoolean("f", f).apply()
        }
    }

    private fun setCity(name: String, lat: Double, lon: Double) {
        cityState.value = name
        latState.value = lat
        lonState.value = lon
        prefs.edit().putString("city", name).putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
    }
}

@Composable
fun WeatherTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, typography = Typography().withFont(GoogleSans), content = content)
}

private fun iconFor(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2, 3 -> Icons.Rounded.WbCloudy
    45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.FlashOn
    else -> Icons.Rounded.WbCloudy
}

private fun dayName(date: String, index: Int): String {
    if (index == 0) return "Today"
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        SimpleDateFormat("EEEE", Locale.getDefault()).format(d!!)
    } catch (e: Exception) {
        date
    }
}

/** The scalloped "cookie" shape from Material 3 Expressive. */
class CookieShape(private val lobes: Int = 9, private val amp: Float = 0.06f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy) / (1f + amp)
        val steps = 180
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
            val rad = r * (1f + amp * cos(lobes * t))
            val x = cx + rad * cos(t)
            val y = cy + rad * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun WeatherScreen(
    city: String,
    lat: Double,
    lon: Double,
    fahrenheit: Boolean,
    onCity: (String, Double, Double) -> Unit,
    onUnit: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf(false) }

    LaunchedEffect(lat, lon, reload) {
        loading = true
        val w = WeatherApi.fetch(lat, lon)
        if (w != null) data = w
        failed = w == null
        loading = false
    }

    val spin = rememberInfiniteTransition(label = "spin")
    val rot by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "rot")

    Box(Modifier.fillMaxSize().background(scheme.surface)) {
        // soft tint at the top, fading into the surface
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.55f), scheme.surface)))
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.surfaceContainerHighest,
                    contentColor = scheme.onSurface,
                    modifier = Modifier.weight(1f).clickable { search = true },
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null)
                        Spacer(Modifier.size(10.dp))
                        Text(city, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                }
                Spacer(Modifier.size(6.dp))
                Surface(shape = CircleShape, color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer) {
                    IconButton(onClick = { reload++ }) { Icon(Icons.Rounded.Refresh, "Refresh") }
                }
            }

            val w = data
            if (w == null) {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loading) CircularProgressIndicator()
                    else Text("Couldn't load the weather. Check your connection.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // ---- hero
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(48.dp))
                        .background(scheme.primaryContainer)
                ) {
                    Box(
                        Modifier
                            .size(240.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 70.dp, y = (-70).dp)
                            .graphicsLayer { rotationZ = rot }
                            .clip(CookieShape(9, 0.07f))
                            .background(scheme.primary.copy(alpha = 0.14f))
                    )
                    Box(
                        Modifier
                            .size(170.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-60).dp, y = 60.dp)
                            .graphicsLayer { rotationZ = -rot }
                            .clip(CookieShape(7, 0.08f))
                            .background(scheme.tertiary.copy(alpha = 0.16f))
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(112.dp).clip(CookieShape(9, 0.07f)).background(scheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(iconFor(w.code, w.isDay), null, Modifier.size(58.dp), tint = scheme.onPrimary)
                        }
                        Text(
                            tempString(w.tempC, fahrenheit),
                            fontSize = 96.sp,
                            lineHeight = 100.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onPrimaryContainer,
                        )
                        Text(
                            conditionLabel(w.code),
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onPrimaryContainer,
                        )
                        w.days.firstOrNull()?.let {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill(Icons.Rounded.ArrowUpward, tempString(it.hiC, fahrenheit))
                                Pill(Icons.Rounded.ArrowDownward, tempString(it.loC, fahrenheit))
                            }
                        }
                    }
                }

                // ---- hourly
                if (w.hours.isNotEmpty()) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = scheme.surfaceContainer) {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text("Next 24 hours", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                w.hours.forEachIndexed { i, h ->
                                    val now = i == 0
                                    Column(
                                        Modifier
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(if (now) scheme.primaryContainer else Color.Transparent)
                                            .padding(horizontal = 12.dp, vertical = 12.dp)
                                            .width(44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(h.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
                                        Icon(iconFor(h.code, h.isDay), null, Modifier.size(26.dp), tint = if (now) scheme.onPrimaryContainer else scheme.primary)
                                        Text(tempString(h.tempC, fahrenheit), style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            if (h.precipPct >= 20) "${h.precipPct}%" else " ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.tertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- details
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Detail(Icons.Rounded.Thermostat, "Feels like", tempString(w.feelsC, fahrenheit), Modifier.weight(1f))
                    Detail(Icons.Rounded.WaterDrop, "Humidity", "${w.humidity}%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Detail(
                        Icons.Rounded.Air, "Wind",
                        if (fahrenheit) "${(w.windKmh * 0.621371).toInt()} mph" else "${w.windKmh.toInt()} km/h",
                        Modifier.weight(1f),
                    )
                    Detail(Icons.Rounded.Umbrella, "Rain chance", "${w.precipPct}%", Modifier.weight(1f))
                }

                // ---- daily
                val lo = w.days.minOfOrNull { it.loC } ?: 0.0
                val hi = w.days.maxOfOrNull { it.hiC } ?: 1.0
                val span = (hi - lo).takeIf { it > 0.1 } ?: 1.0
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = scheme.surfaceContainer) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text("7-day forecast", Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
                        w.days.forEachIndexed { i, d ->
                            val start = ((d.loC - lo) / span).toFloat().coerceIn(0f, 1f)
                            val width = ((d.hiC - d.loC) / span).toFloat().coerceAtLeast(0.06f).coerceAtMost(1f - start).coerceAtLeast(0.02f)
                            val rest = 1f - start - width
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(dayName(d.date, i), Modifier.weight(1.1f), style = MaterialTheme.typography.bodyLarge)
                                Icon(iconFor(d.code, true), null, Modifier.size(24.dp), tint = scheme.primary)
                                Text(
                                    tempString(d.loC, fahrenheit),
                                    Modifier.width(48.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                                Row(
                                    Modifier.weight(1.3f).height(8.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                                ) {
                                    if (start > 0.001f) Spacer(Modifier.weight(start))
                                    Box(Modifier.weight(width).fillMaxHeight().clip(CircleShape).background(scheme.primary))
                                    if (rest > 0.001f) Spacer(Modifier.weight(rest))
                                }
                                Text(
                                    tempString(d.hiC, fahrenheit),
                                    Modifier.width(48.dp),
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = !fahrenheit, onClick = { onUnit(false) }, label = { Text("°C") })
                FilterChip(selected = fahrenheit, onClick = { onUnit(true) }, label = { Text("°F") })
                Text("Data: Open-Meteo", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.navigationBarsPadding())
        }
    }

    if (search) {
        SearchDialog(onDismiss = { search = false }, onPick = { r ->
            search = false
            onCity(r.name, r.lat, r.lon)
        })
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(50), color = scheme.surface.copy(alpha = 0.55f), contentColor = scheme.onPrimaryContainer) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Detail(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(32.dp), color = scheme.surfaceContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CookieShape(8, 0.07f)).background(scheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(22.dp), tint = scheme.onTertiaryContainer)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchDialog(onDismiss: () -> Unit, onPick: (GeoResult) -> Unit) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search a city") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = q, onValueChange = { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FilledTonalButton(
                    enabled = !busy && q.isNotBlank(),
                    onClick = { busy = true; scope.launch { results = WeatherApi.search(q); busy = false } },
                ) { Text(if (busy) "Searching…" else "Search") }
                results.forEach { r ->
                    Text(
                        r.display,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(r) }.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)
