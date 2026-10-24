package com.kev.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class TvApp(
    val packageName: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val intent: Intent,
    // User-installed, so it can be uninstalled (built-in apps can only be disabled).
    val removable: Boolean,
)

enum class ShortcutKind { LIVE_TV, INPUT, NETWORK, SOUND, SETTINGS }

data class Shortcut(
    val id: String,
    val label: String,
    val kind: ShortcutKind,
    val intent: Intent,
    // Inputs only: something is plugged in and powered on.
    val connected: Boolean = false,
)

// Pinned to the front of the home row, in this order; everything else lives on the All apps page.
val FAVOURITE_PACKAGES = listOf(
    "com.dstvmobile.android",
    "com.netflix.ninja",
    "com.google.android.youtube.tv",
)

private const val LIVE_TV_PACKAGE = "com.mediatek.wwtv.tvcenter"

// Shown in the Watch row instead of the app grid.
private val HIDDEN_PACKAGES = setOf(
    LIVE_TV_PACKAGE,
    "com.android.tv.settings",
)

fun loadApps(context: Context): List<TvApp> {
    val pm = context.packageManager
    val hidden = HIDDEN_PACKAGES + context.packageName
    val leanback = queryMain(pm, Intent.CATEGORY_LEANBACK_LAUNCHER)
    // Phone-style entries only for user-installed apps: on this TV the system ones are internal
    // tools (disclaimer screens, Netflix helpers) that aren't meant to be opened.
    val phone = queryMain(pm, Intent.CATEGORY_LAUNCHER)
        .filter { it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 }

    // Prefer the TV (leanback) entry point when an app has both.
    return (leanback.map { it to Intent.CATEGORY_LEANBACK_LAUNCHER } + phone.map { it to Intent.CATEGORY_LAUNCHER })
        .distinctBy { (info, _) -> info.activityInfo.packageName }
        .filter { (info, _) -> info.activityInfo.packageName !in hidden }
        .map { (info, category) -> info.toTvApp(pm, category) }
        .sortedBy { it.label.lowercase() }
}

private fun queryMain(pm: PackageManager, category: String): List<ResolveInfo> =
    pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

private fun ResolveInfo.toTvApp(pm: PackageManager, category: String): TvApp {
    val activity = activityInfo
    val banner = activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm)
    val icon = runCatching { loadIcon(pm) }.getOrNull()
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(category)
        .setClassName(activity.packageName, activity.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    return TvApp(
        packageName = activity.packageName,
        label = loadLabel(pm).toString(),
        // Fixed sizes keep memory bounded regardless of how large the app's artwork is.
        banner = banner?.let { runCatching { it.toBitmap(480, 270).asImageBitmap() }.getOrNull() },
        icon = icon?.let { runCatching { it.toBitmap(144, 144).asImageBitmap() }.getOrNull() },
        intent = intent,
        removable = activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0,
    )
}

fun loadShortcuts(context: Context): List<Shortcut> {
    val shortcuts = mutableListOf<Shortcut>()
    val pm = context.packageManager

    val liveTv = pm.getLeanbackLaunchIntentForPackage(LIVE_TV_PACKAGE)
        ?: pm.getLaunchIntentForPackage(LIVE_TV_PACKAGE)
    if (liveTv != null) {
        shortcuts += Shortcut("live", "Live TV", ShortcutKind.LIVE_TV, liveTv)
    }

    val inputManager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
    inputManager?.tvInputList.orEmpty()
        // Top-level physical inputs only; HDMI-CEC devices show up as children of their port.
        .filter { it.isPassthroughInput && it.parentId == null }
        .sortedWith(compareBy<TvInputInfo> { it.type != TvInputInfo.TYPE_HDMI }.thenBy { it.loadLabel(context).toString() })
        .forEach { input ->
            val label = input.loadCustomLabel(context)?.toString() ?: input.loadLabel(context).toString()
            val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
            val connected = inputManager?.getInputState(input.id) == TvInputManager.INPUT_STATE_CONNECTED
            shortcuts += Shortcut(input.id, label, ShortcutKind.INPUT, intent, connected)
        }

    // Not every TV build has every settings screen (this Hisense has no Bluetooth or Display page),
    // so only offer the ones that will actually open.
    listOf(
        // On Android TV this opens the Network page, which covers both Ethernet and Wi-Fi.
        Shortcut("network", "Network", ShortcutKind.NETWORK, Intent(Settings.ACTION_WIFI_SETTINGS)),
        Shortcut("sound", "Sound", ShortcutKind.SOUND, Intent(Settings.ACTION_SOUND_SETTINGS)),
        Shortcut("settings", "Settings", ShortcutKind.SETTINGS, Intent(Settings.ACTION_SETTINGS)),
    ).filterTo(shortcuts) { pm.resolveActivity(it.intent, 0) != null }
    return shortcuts
}
