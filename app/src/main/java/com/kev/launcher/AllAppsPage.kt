@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import java.text.SimpleDateFormat
import java.util.Locale

/** Everything that isn't pinned to the home row, as a grid over the darkened slideshow. */
@Composable
fun AllAppsPage(apps: List<TvApp>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val firstApp = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstApp.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.7f))) {
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = EDGE), verticalAlignment = Alignment.CenterVertically) {
                Wordmark(fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                SmallClock()
            }

            Column(Modifier.padding(start = EDGE, top = 22.dp)) {
                Text(
                    "LIBRARY",
                    style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.3.em, color = GoldLight),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "All apps",
                        style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Light, fontSize = 42.sp, color = Color.White, shadow = SoftShadow),
                    )
                    Text(
                        "${apps.size}",
                        style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = GoldLight),
                        modifier = Modifier.padding(start = 12.dp, bottom = 9.dp),
                    )
                }
                Text(
                    "Hold OK on an app for options  ·  BACK to return home",
                    style = TextStyle(fontFamily = UiFont, fontSize = 12.sp, letterSpacing = 0.04.em, color = Color.White.copy(alpha = 0.5f)),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(start = EDGE, end = EDGE, top = 24.dp, bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(app, focusRequester = if (index == 0) firstApp else null)
                }
            }
        }
    }
}

@Composable
private fun SmallClock() {
    val context = LocalContext.current
    val now = rememberNow()
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    Column(horizontalAlignment = Alignment.End) {
        Text(
            SimpleDateFormat(pattern, Locale.getDefault()).format(now),
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Light, fontSize = 26.sp, color = Color.White, shadow = SoftShadow),
        )
        Text(
            SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(now).uppercase(),
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = 0.24.em, color = GoldLight),
        )
    }
}
