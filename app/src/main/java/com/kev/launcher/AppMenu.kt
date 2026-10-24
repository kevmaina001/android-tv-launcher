@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Hold OK on an app: open it, see its info (storage, cache, force stop) or uninstall it. */
@Composable
fun AppMenu(app: TvApp, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val packageUri = Uri.parse("package:${app.packageName}")
    val shape = RoundedCornerShape(18.dp)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(320.dp)
                .background(Color(0xF20C0C11), shape)
                .border(1.dp, Gold.copy(alpha = 0.35f), shape)
                .padding(10.dp),
        ) {
            Row(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(72.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E1E27)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        app.banner != null -> Image(app.banner, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        app.icon != null -> Image(app.icon, null, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "APP",
                        style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = 0.3.em, color = GoldLight),
                    )
                    Text(
                        app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = Color.White),
                    )
                }
            }

            MenuItem("Open", Icons.Filled.PlayArrow, Modifier.focusRequester(first)) {
                onDismiss()
                launch(context, app.intent)
            }
            MenuItem("App info", Icons.Filled.Info) {
                onDismiss()
                launch(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            // Built-in apps can't be removed; App info still lets you disable them.
            if (app.removable) {
                MenuItem("Uninstall", Icons.Filled.Delete) {
                    onDismiss()
                    // The system asks to confirm; the grid refreshes itself once the package is gone.
                    launch(context, Intent(Intent.ACTION_DELETE, packageUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(46.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Gold,
            contentColor = Color.White,
            focusedContentColor = Ink,
        ),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.02.em))
        }
    }
}
