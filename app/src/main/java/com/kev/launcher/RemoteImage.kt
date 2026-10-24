package com.kev.launcher

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Posters for the Continue watching rows: from the apps' CDNs (https) or their own resources. */
object ImageLoader {
    // About 24 MB of decoded posters: plenty for two rows of 20.
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    suspend fun load(context: Context, uri: String, targetWidth: Int): ImageBitmap? {
        cache.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                // Read bounds first so a 2000px poster is decoded at roughly tile size.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                open(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                open(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
            }.getOrNull()?.also { cache.put(uri, it) }
        }
    }

    private fun open(context: Context, uri: String): InputStream? {
        val parsed = Uri.parse(uri)
        return when (parsed.scheme) {
            "http", "https" -> (URL(uri).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }.inputStream
            "content", "android.resource", "file" -> context.contentResolver.openInputStream(parsed)
            else -> null
        }
    }
}

@Composable
fun RemoteImage(uri: String?, contentDescription: String?, targetWidth: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = uri?.let { ImageLoader.load(context, it, targetWidth) }
    }
    bitmap?.let { Image(it, contentDescription, modifier, contentScale = ContentScale.Crop) }
}
