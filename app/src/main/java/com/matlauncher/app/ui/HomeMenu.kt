package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.matlauncher.app.data.IconStyle
import com.matlauncher.app.data.LauncherSettings

/** Shown when you long-press the home screen: quick icon style, wallpaper and settings. */
@Composable
fun HomeMenu(settings: LauncherSettings, actions: LauncherActions) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    actions.closeOverlay()
                }
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = scheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
                Text("App icons", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StyleTile("Normal", Icons.Rounded.Apps, settings.iconStyle == IconStyle.ORIGINAL, Modifier.weight(1f)) {
                        settings.iconStyle = IconStyle.ORIGINAL
                    }
                    StyleTile("Themed", Icons.Rounded.Palette, settings.iconStyle == IconStyle.THEMED, Modifier.weight(1f)) {
                        settings.iconStyle = IconStyle.THEMED
                    }
                    StyleTile("Transparent", Icons.Rounded.Opacity, settings.iconStyle == IconStyle.TRANSPARENT, Modifier.weight(1f)) {
                        settings.iconStyle = IconStyle.TRANSPARENT
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile("Wallpaper", Icons.Rounded.Wallpaper, Modifier.weight(1f)) {
                        actions.closeOverlay()
                        actions.showWallpaperGallery()
                    }
                    ActionTile("Widgets", Icons.Rounded.Widgets, Modifier.weight(1f)) {
                        actions.openWidgetPicker()
                    }
                    ActionTile("Edit home", Icons.Rounded.Edit, Modifier.weight(1f)) {
                        actions.editHome()
                    }
                    ActionTile("Settings", Icons.Rounded.Settings, Modifier.weight(1f)) {
                        actions.openSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleTile(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainerHighest,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
