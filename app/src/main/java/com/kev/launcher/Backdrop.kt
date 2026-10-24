package com.kev.launcher

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Photos are from Wikimedia Commons; their licences require the credit shown on screen (see CREDITS.md).
data class Backdrop(@DrawableRes val res: Int, val place: String, val credit: String)

val BACKDROPS = listOf(
    Backdrop(R.drawable.bg_hongkong, "Victoria Harbour, Hong Kong", "Benh Lieu Song · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_serengeti, "Sunrise over the Serengeti", "Giles Laurent · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_fullerton, "The Fullerton Hotel, Singapore", "Basile Morin · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_kilimanjaro, "Kilimanjaro from Amboseli", "Sergey Pesterev · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_madinat, "Madinat Jumeirah, Dubai", "Diego Delso · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_namib, "Namib-Naukluft, Namibia", "Yathin S Krishnappa · CC BY-SA 3.0"),
    Backdrop(R.drawable.bg_singapore, "Marina Bay at dusk, Singapore", "Basile Morin · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_stmoritz, "St. Moritz, Switzerland", "Isiwal · CC BY-SA 4.0"),
    Backdrop(R.drawable.bg_ladigue, "La Digue, Seychelles", "Giles Laurent · CC BY-SA 4.0"),
)

private const val SLIDE_MS = 30_000L
private const val FADE_MS = 2_500

private class Slide(val index: Int, val bitmap: ImageBitmap)

/**
 * Full-screen photo slideshow with crossfades. The photos stay still between fades: a continuous
 * zoom redrew the whole screen every frame, which this TV's chip can't spare (40% CPU at idle).
 */
@Composable
fun BackdropSlideshow(onBackdropChange: (Backdrop) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val notify by rememberUpdatedState(onBackdropChange)
    var slide by remember { mutableStateOf<Slide?>(null) }

    LaunchedEffect(lifecycle) {
        // Start somewhere different each time the launcher is created.
        var index = ((System.currentTimeMillis() / SLIDE_MS) % BACKDROPS.size).toInt()
        // Only cycle while the home screen is visible, not behind Netflix.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val bitmap = withContext(Dispatchers.IO) { decode(context, BACKDROPS[index].res) }
                slide = Slide(index, bitmap)
                notify(BACKDROPS[index])
                delay(SLIDE_MS)
                index = (index + 1) % BACKDROPS.size
            }
        }
    }

    Crossfade(targetState = slide, animationSpec = tween(FADE_MS), label = "backdrop", modifier = modifier) { current ->
        if (current != null) {
            Image(bitmap = current.bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Decodes a photo with the scrims painted in: a wash from the left behind the clock and a fade at
 * the bottom behind the rows. Baking them in once means each frame fills the screen once, not three times.
 */
private fun decode(context: Context, @DrawableRes res: Int): ImageBitmap {
    val bitmap = BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inMutable = true })
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val canvas = Canvas(bitmap)
    val paint = Paint()
    paint.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(ink(0.82f), ink(0.35f), ink(0f)), floatArrayOf(0f, 0.42f, 0.75f), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, w, h, paint)
    paint.shader = LinearGradient(
        0f, 0f, 0f, h,
        intArrayOf(ink(0.72f), ink(0f), ink(0f), ink(0.78f), ink(0.96f)),
        floatArrayOf(0f, 0.22f, 0.48f, 0.72f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w, h, paint)
    return bitmap.asImageBitmap()
}

private fun ink(alpha: Float) = Ink.copy(alpha = alpha).toArgb()
