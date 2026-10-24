@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Date

val EDGE = 48.dp

private val TileShape = RoundedCornerShape(10.dp)
private val TileBase = Brush.linearGradient(listOf(Color(0xFF1E1E27), Color(0xFF0D0D12)))
// A faint highlight across the top of every tile, like light catching glass.
private val TileSheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.45f to Color.Transparent)

/** The current time, updated on each second boundary. */
@Composable
fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }
    return now
}

/** "◆ NAME" in brushed gold: the viewer's name, or HOME until one is set. */
@Composable
fun Wordmark(fontSize: TextUnit = 24.sp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).rotate(45f).background(GoldBrush))
        Spacer(Modifier.width(12.dp))
        Text(
            Profile.name.ifBlank { "Home" }.uppercase(),
            style = TextStyle(
                brush = GoldBrush,
                fontFamily = UiFont,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.32.em,
                shadow = SoftShadow,
            ),
        )
    }
}

/** "APPS ──": small tracked caps and a gold rule. */
@Composable
fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = EDGE), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.3.em, color = Color.White.copy(alpha = 0.62f)),
        )
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(40.dp).height(1.dp).background(Gold.copy(alpha = 0.7f)))
    }
}

/** Glass pill that turns solid gold when focused. */
@Composable
fun Chip(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    // A small status light after the label, e.g. green for connected.
    statusColor: Color? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.9f),
            focusedContentColor = Ink,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))),
            focusedBorder = Border(BorderStroke(1.dp, GoldLight)),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(if (focused) GoldBrush else Brush.linearGradient(listOf(Ink.copy(alpha = 0.45f), Ink.copy(alpha = 0.45f))))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text(label.uppercase(), style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em))
            if (statusColor != null) {
                Spacer(Modifier.width(9.dp))
                Box(Modifier.size(7.dp).background(statusColor, CircleShape))
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(4.dp))
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * An app as a compact 16:9 card with its name underneath. [modifier] sizes the whole tile;
 * [focusRequester] goes on the focusable card itself.
 */
@Composable
fun AppTile(app: TvApp, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen) AppMenu(app, onDismiss = { menuOpen = false })
    val labelColor by animateColorAsState(if (focused) GoldLight else Color.White.copy(alpha = 0.66f), tween(200), label = "label")

    Column(modifier) {
        TileCard(
            onClick = { launch(context, app.intent) },
            onLongClick = { menuOpen = true },
            focused = focused,
            onFocusChange = { focused = it },
            focusRequester = focusRequester,
        ) {
            if (app.banner != null) {
                Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (app.icon != null) {
                // No TV banner: the app icon centred on a dark card.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(app.icon, contentDescription = app.label, modifier = Modifier.size(40.dp))
                }
            }
        }
        Text(
            app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.02.em, color = labelColor),
            modifier = Modifier.padding(top = 10.dp, start = 2.dp),
        )
    }
}

/** The last tile on the home row: opens the All apps page. */
@Composable
fun MoreAppsTile(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    val labelColor by animateColorAsState(if (focused) GoldLight else Color.White.copy(alpha = 0.66f), tween(200), label = "label")

    Column(modifier) {
        TileCard(onClick = onClick, focused = focused, onFocusChange = { focused = it }, focusRequester = focusRequester, dimWhenIdle = false) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(KevIcons.Apps, contentDescription = null, tint = GoldLight, modifier = Modifier.size(26.dp))
                Text(
                    "+$count",
                    style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.1.em, color = GoldLight),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Text(
            "All apps",
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.02.em, color = labelColor),
            modifier = Modifier.padding(top = 10.dp, start = 2.dp),
        )
    }
}

/** A Continue watching card: poster, a gold progress bar when the app reports one, and the title below. */
@Composable
fun RecentCard(item: RecentItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val titleColor by animateColorAsState(if (focused) GoldLight else Color.White.copy(alpha = 0.8f), tween(200), label = "title")

    Column(modifier) {
        TileCard(onClick = { launch(context, item.intent) }, focused = focused, onFocusChange = { focused = it }, focusRequester = null) {
            RemoteImage(item.imageUri, item.title, targetWidth = 480, modifier = Modifier.fillMaxSize())
            item.progress?.let { progress ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(GoldBrush))
                }
            }
        }
        Text(
            item.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = titleColor),
            modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
        )
        item.subtitle?.let {
            Text(
                it,
                maxLines = 1,
                style = TextStyle(fontFamily = UiFont, fontSize = 10.sp, letterSpacing = 0.04.em, color = Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.padding(top = 3.dp, start = 2.dp),
            )
        }
    }
}

@Composable
private fun TileCard(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester?,
    dimWhenIdle: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    // Unfocused tiles sit back in the dark; the focused one comes forward at full brightness.
    val dim by animateFloatAsState(if (focused || !dimWhenIdle) 0f else 0.3f, tween(220), label = "dim")
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChange(it.isFocused) },
        shape = ClickableSurfaceDefaults.shape(TileShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))),
            focusedBorder = Border(BorderStroke(2.dp, GoldBrush)),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContentColor = Color.White,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(TileBase)) {
            content()
            Box(Modifier.fillMaxSize().background(TileSheen))
            Box(Modifier.fillMaxSize().alpha(dim).background(Ink))
        }
    }
}

fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Couldn't open that", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, "Couldn't open that", Toast.LENGTH_SHORT).show()
    }
}
