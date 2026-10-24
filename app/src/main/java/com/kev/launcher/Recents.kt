package com.kev.launcher

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log

data class RecentItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUri: String?,
    val intent: Intent,
    // 0..1 when the app reports how far in you are.
    val progress: Float?,
)

data class RecentRow(
    val packageName: String,
    val appLabel: String,
    val heading: String,
    val items: List<RecentItem>,
)

private const val TAG = "KevRecents"
private const val MAX_ITEMS = 20
private val WATCH_NEXT = Uri.parse("content://android.media.tv/watch_next_program")
private val PREVIEW_PROGRAMS = Uri.parse("content://android.media.tv/preview_program")

/**
 * Rows for the favourite apps from the TV provider. An app's own Watch Next list (true history,
 * e.g. YouTube) wins; otherwise fall back to its preview channel (e.g. Netflix only publishes picks).
 *
 * Needs READ_TV_LISTINGS. The provider rejects selections from apps without ACCESS_ALL_EPG_DATA,
 * so whole tables are read and filtered here; they hold a few dozen rows.
 */
fun loadRecents(context: Context): List<RecentRow> {
    val watchNext = readTable(context, WATCH_NEXT)
    val preview = readTable(context, PREVIEW_PROGRAMS)
    val pm = context.packageManager

    return FAVOURITE_PACKAGES.mapNotNull { pkg ->
        val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull() ?: return@mapNotNull null
        val history = watchNext.filter { it.pkg == pkg }.sortedByDescending { it.lastEngaged }
        val (rows, heading) = if (history.isNotEmpty()) history to "Continue watching" else preview.filter { it.pkg == pkg } to "Picks for you"
        val items = rows.mapNotNull { it.toItem() }.take(MAX_ITEMS)
        if (items.isEmpty()) null else RecentRow(pkg, label, heading, items)
    }
        // History rows first, then picks.
        .sortedBy { if (it.heading == "Continue watching") 0 else 1 }
}

private class ProgramRow(
    val id: Long,
    val pkg: String,
    val title: String?,
    val episodeTitle: String?,
    val poster: String?,
    val thumbnail: String?,
    val intentUri: String?,
    val position: Long,
    val duration: Long,
    val lastEngaged: Long,
)

private fun readTable(context: Context, uri: Uri): List<ProgramRow> = try {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        buildList {
            while (c.moveToNext()) {
                add(
                    ProgramRow(
                        id = c.long("_id"),
                        pkg = c.string("package_name") ?: continue,
                        title = c.string("title"),
                        episodeTitle = c.string("episode_title"),
                        poster = c.string("poster_art_uri"),
                        thumbnail = c.string("thumbnail_uri"),
                        intentUri = c.string("intent_uri"),
                        position = c.long("last_playback_position_millis"),
                        duration = c.long("duration_millis"),
                        lastEngaged = c.long("last_engagement_time_utc_millis"),
                    ),
                )
            }
        }
    }.orEmpty()
} catch (e: SecurityException) {
    // Permission not granted yet; the row simply doesn't appear.
    Log.w(TAG, "No access to $uri: ${e.message}")
    emptyList()
}

private fun ProgramRow.toItem(): RecentItem? {
    val title = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val intent = launchIntent() ?: return null
    val progress = if (duration > 0 && position > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else null
    val subtitle = if (duration > 0 && position > 0) timeLeft(duration - position) else episodeTitle
    return RecentItem("$pkg:$id", title, subtitle, poster ?: thumbnail, intent, progress)
}

private fun ProgramRow.launchIntent(): Intent? {
    val intent = intentUri?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() } ?: return null
    // These intents are written by other apps; only ever launch into the app that published them.
    val target = intent.component?.packageName
    if (target != null && target != pkg) return null
    intent.selector = null
    if (target == null) intent.setPackage(pkg)
    return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun timeLeft(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(1)
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min left" else "$minutes min left"
}

private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let { getString(it) }
private fun Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let { getLong(it) } ?: 0L
