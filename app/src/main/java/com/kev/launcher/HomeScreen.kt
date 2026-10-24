@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Page { HOME, ALL_APPS }

private val HomeTileWidth = 160.dp
private val RecentCardWidth = 224.dp
private val Online = Color(0xFF6FCF97)
private val Offline = Color(0xFFEB5757)

@Composable
fun HomeScreen(refreshKey: Int, homePresses: Int, recentsKey: Int) {
    val context = LocalContext.current
    val apps by produceState(emptyList<TvApp>(), refreshKey) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }
    val shortcuts by produceState(emptyList<Shortcut>(), refreshKey) {
        value = withContext(Dispatchers.IO) { loadShortcuts(context) }
    }
    val recents by produceState(emptyList<RecentRow>(), recentsKey, refreshKey) {
        value = withContext(Dispatchers.IO) { loadRecents(context) }
    }
    val favourites = FAVOURITE_PACKAGES.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.ifEmpty { apps.take(3) }
    val otherApps = apps.filterNot { it in favourites }

    var page by remember { mutableStateOf(Page.HOME) }
    // Coming back from All apps should land on the tile that opened it, not the first favourite.
    var returnedFromAllApps by remember { mutableStateOf(false) }
    LaunchedEffect(homePresses) { page = Page.HOME }
    var backdrop by remember { mutableStateOf<Backdrop?>(null) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        BackdropSlideshow(onBackdropChange = { backdrop = it }, modifier = Modifier.fillMaxSize())

        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "page",
        ) { shown ->
            when (shown) {
                Page.HOME -> HomePage(
                    backdrop = backdrop,
                    favourites = favourites,
                    moreCount = otherApps.size,
                    shortcuts = shortcuts,
                    recents = recents,
                    homePresses = homePresses,
                    focusMoreTile = returnedFromAllApps,
                    onOpenAllApps = { page = Page.ALL_APPS },
                )
                Page.ALL_APPS -> AllAppsPage(
                    apps = otherApps,
                    onBack = {
                        returnedFromAllApps = true
                        page = Page.HOME
                    },
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    backdrop: Backdrop?,
    favourites: List<TvApp>,
    moreCount: Int,
    shortcuts: List<Shortcut>,
    recents: List<RecentRow>,
    homePresses: Int,
    focusMoreTile: Boolean,
    onOpenAllApps: () -> Unit,
) {
    val context = LocalContext.current
    val firstFavourite = remember { FocusRequester() }
    val moreTile = remember { FocusRequester() }
    LaunchedEffect(favourites.isNotEmpty()) {
        if (favourites.isNotEmpty()) {
            withFrameNanos { }
            runCatching { (if (focusMoreTile && moreCount > 0) moreTile else firstFavourite).requestFocus() }
        }
    }

    val scroll = rememberScrollState()
    // HOME while scrolled down goes back to the top.
    LaunchedEffect(homePresses) {
        if (scroll.value > 0) {
            runCatching { firstFavourite.requestFocus() }
            scroll.animateScrollTo(0)
        }
    }

    val inputs = shortcuts.filter { it.kind == ShortcutKind.INPUT }
    val network = rememberNetworkKind()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screen = maxHeight
        // The photo fades to near-black as Continue watching slides up, so the posters stand out.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val max = scroll.maxValue
                if (scroll.value > 0 && max in 1 until Int.MAX_VALUE) drawRect(Ink, alpha = 0.8f * scroll.value / max)
            },
        )

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // The home page proper is exactly one screen; Continue watching sits just below it.
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(screen)
                    .bringWholeIntoView()
                    .padding(top = 28.dp, bottom = 16.dp),
            ) {
                TopBar(backdrop)
                Hero(Modifier.padding(start = EDGE, top = 16.dp))
                Spacer(Modifier.weight(1f))

                SectionLabel("Quick access")
                Row(
                    modifier = Modifier.padding(start = EDGE, end = EDGE, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    shortcuts.firstOrNull { it.kind == ShortcutKind.LIVE_TV }?.let { Chip(it.label, KevIcons.Tv, onClick = { launch(context, it.intent) }) }
                    if (inputs.isNotEmpty()) InputsChip(inputs)
                    shortcuts.firstOrNull { it.kind == ShortcutKind.NETWORK }?.let { NetworkChip(network, onClick = { launch(context, it.intent) }) }
                    shortcuts.filter { it.kind in setOf(ShortcutKind.SOUND, ShortcutKind.SETTINGS) }.forEach { shortcut ->
                        val icon = if (shortcut.kind == ShortcutKind.SOUND) KevIcons.Sound else Icons.Filled.Settings
                        Chip(shortcut.label, icon, onClick = { launch(context, shortcut.intent) })
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Apps")
                    Spacer(Modifier.weight(1f))
                    if (recents.isNotEmpty()) ScrollHint(Modifier.padding(end = EDGE))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = EDGE, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    itemsIndexed(favourites, key = { _, app -> app.packageName }) { index, app ->
                        AppTile(app, Modifier.width(HomeTileWidth), focusRequester = if (index == 0) firstFavourite else null)
                    }
                    if (moreCount > 0) {
                        item(key = "all-apps") {
                            MoreAppsTile(moreCount, onClick = onOpenAllApps, modifier = Modifier.width(HomeTileWidth), focusRequester = moreTile)
                        }
                    }
                }
            }

            if (recents.isNotEmpty()) {
                RecentsSection(
                    recents,
                    Modifier.height(screen).bringWholeIntoView(),
                )
            }
        }
    }
}

/** "Continue watching" and a gold chevron at the right of the Apps label: there's more below. */
@Composable
private fun ScrollHint(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "CONTINUE WATCHING",
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.24.em, color = Color.White.copy(alpha = 0.5f)),
        )
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = GoldLight, modifier = Modifier.padding(start = 4.dp).size(18.dp))
    }
}

/** Shows how the TV is online (cable or Wi-Fi) and opens the network settings. */
@Composable
private fun NetworkChip(kind: NetworkKind, onClick: () -> Unit) {
    when (kind) {
        NetworkKind.ETHERNET -> Chip("Ethernet", KevIcons.Ethernet, onClick, statusColor = Online)
        NetworkKind.WIFI -> Chip("Wi-Fi", KevIcons.Wifi, onClick, statusColor = Online)
        NetworkKind.OFFLINE -> Chip("Offline", KevIcons.Wifi, onClick, statusColor = Offline)
    }
}

@Composable
private fun RecentsSection(rows: List<RecentRow>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 28.dp)) {
        Text(
            "Continue watching",
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 26.sp, color = Color.White, shadow = SoftShadow),
            modifier = Modifier.padding(start = EDGE, bottom = 16.dp),
        )
        rows.forEach { row ->
            SectionLabel("${row.appLabel}  ·  ${row.heading}")
            LazyRow(
                contentPadding = PaddingValues(start = EDGE, end = EDGE, top = 14.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(row.items, key = { it.id }) { item ->
                    RecentCard(item, Modifier.width(RecentCardWidth))
                }
            }
        }
    }
}

/** The wordmark on the home page doubles as the way to change the name; it also asks once on first run. */
@Composable
private fun EditableWordmark() {
    var editing by remember { mutableStateOf(!Profile.asked) }
    if (editing) NameDialog(onDismiss = { editing = false })
    Surface(
        onClick = { editing = true },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
        ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.dp, GoldLight))),
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) { Wordmark() }
    }
}

/** One "Inputs" chip in place of a chip per port; the ports open in a menu above it. */
@Composable
private fun InputsChip(inputs: List<Shortcut>) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var open by remember { mutableStateOf(false) }
    var wasOpened by remember { mutableStateOf(false) }
    val chipFocus = remember { FocusRequester() }

    LaunchedEffect(open) {
        // Hand focus back to the chip when the menu closes (but not on first composition).
        if (!open && wasOpened) {
            withFrameNanos { }
            runCatching { chipFocus.requestFocus() }
        }
    }

    Box {
        Chip(
            "Inputs",
            KevIcons.Input,
            onClick = {
                open = true
                wasOpened = true
            },
            modifier = Modifier.focusRequester(chipFocus),
            trailingIcon = Icons.Filled.ArrowDropDown,
        )
        if (open) {
            // The chips sit near the bottom of the screen, so the menu opens upwards.
            Popup(
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, with(density) { -54.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                InputsMenu(inputs, onPick = {
                    open = false
                    launch(context, it.intent)
                })
            }
        }
    }
}

@Composable
private fun InputsMenu(inputs: List<Shortcut>, onPick: (Shortcut) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .width(240.dp)
            .background(Color(0xF20C0C11), shape)
            .border(1.dp, Gold.copy(alpha = 0.35f), shape)
            .padding(8.dp),
    ) {
        Text(
            "SOURCE",
            style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.3.em, color = GoldLight),
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 8.dp),
        )
        inputs.forEachIndexed { index, input ->
            Surface(
                onClick = { onPick(input) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .then(if (index == 0) Modifier.focusRequester(first) else Modifier),
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
                    Text(input.label, style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))
                    Spacer(Modifier.weight(1f))
                    if (input.connected) {
                        Box(Modifier.size(7.dp).background(Color(0xFF6FCF97), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Connected", style = TextStyle(fontFamily = UiFont, fontSize = 10.sp, letterSpacing = 0.04.em))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(backdrop: Backdrop?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = EDGE - 10.dp, end = EDGE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditableWordmark()
        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = backdrop,
            transitionSpec = { fadeIn(tween(1200)) togetherWith fadeOut(tween(800)) },
            label = "caption",
        ) { shown ->
            if (shown != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        shown.place,
                        style = TextStyle(
                            fontFamily = UiFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            shadow = SoftShadow,
                        ),
                    )
                    Text(
                        "PHOTO · " + shown.credit.uppercase(),
                        style = TextStyle(
                            fontFamily = UiFont,
                            fontSize = 8.sp,
                            letterSpacing = 0.14.em,
                            color = Color.White.copy(alpha = 0.6f),
                            shadow = SoftShadow,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now = rememberNow()
    val is24h = DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
    val amPm = if (is24h) null else SimpleDateFormat("a", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE · d MMMM", Locale.getDefault()).format(now).uppercase()

    Column(modifier) {
        Text(
            date,
            style = TextStyle(
                fontFamily = UiFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.28.em,
                color = GoldLight,
                shadow = SoftShadow,
            ),
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                time,
                style = TextStyle(
                    fontFamily = UiFont,
                    fontWeight = FontWeight.Light,
                    fontSize = 88.sp,
                    lineHeight = 88.sp,
                    color = Color.White,
                    shadow = SoftShadow,
                    // Trim the font's built-in padding so the clock sits tight to the date.
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
                modifier = Modifier.padding(top = 6.dp),
            )
            if (amPm != null) {
                Text(
                    amPm.uppercase(),
                    style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Light, fontSize = 18.sp, letterSpacing = 0.2.em, color = Color.White.copy(alpha = 0.75f)),
                    modifier = Modifier.padding(start = 10.dp, bottom = 14.dp),
                )
            }
        }
        Text(
            if (Profile.name.isBlank()) "${greeting(now)}." else "${greeting(now)}, ${Profile.name}.",
            style = TextStyle(
                fontFamily = UiFont,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                color = Color.White.copy(alpha = 0.9f),
                shadow = SoftShadow,
            ),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun greeting(now: Date): String {
    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

/**
 * When anything inside gets focus, scroll so this whole section is on screen rather than just the
 * focused card, so the page moves between the home screen and Continue watching in one step.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.bringWholeIntoView(): Modifier = composed {
    var size by remember { mutableStateOf(Size.Zero) }
    val responder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect) = Rect(Offset.Zero, size)
            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        }
    }
    onSizeChanged { size = it.toSize() }.bringIntoViewResponder(responder)
}
