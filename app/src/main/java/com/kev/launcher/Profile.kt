@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** The viewer's name, shown as the wordmark and in the greeting. Blank means "not set". */
object Profile {
    private const val PREFS = "profile"
    private const val KEY_NAME = "name"
    private const val KEY_ASKED = "asked"
    private const val MAX_LENGTH = 16

    var name by mutableStateOf("")
        private set

    // Whether the first-run "What's your name?" prompt has been answered or skipped.
    var asked by mutableStateOf(true)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        name = prefs.getString(KEY_NAME, "").orEmpty()
        asked = prefs.getBoolean(KEY_ASKED, false)
    }

    fun save(context: Context, newName: String) {
        name = newName.trim().take(MAX_LENGTH)
        asked = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NAME, name)
            .putBoolean(KEY_ASKED, true)
            .apply()
    }

    fun clean(input: String) = input.take(MAX_LENGTH)
}

/** Asks for (or changes) the name. Saving a blank name turns personalisation off. */
@Composable
fun NameDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(Profile.name) }
    var fieldFocused by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val saveButton = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { field.requestFocus() }
    }
    val save = {
        Profile.save(context, text)
        onDismiss()
    }
    val shape = RoundedCornerShape(18.dp)

    Dialog(
        onDismissRequest = {
            // BACK on the first-run prompt counts as "skip" so it doesn't come back every time.
            Profile.save(context, Profile.name)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .width(420.dp)
                .background(Color(0xF20C0C11), shape)
                .border(1.dp, Gold.copy(alpha = 0.35f), shape)
                .padding(28.dp),
        ) {
            Text(
                "WELCOME",
                style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.3.em, color = GoldLight),
            )
            Text(
                "What should we call you?",
                style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, color = Color.White),
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "Shown at the top of the home screen and in the greeting. Change it any time by selecting the name.",
                style = TextStyle(fontFamily = UiFont, fontSize = 12.sp, lineHeight = 17.sp, color = Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.padding(top = 8.dp),
            )

            val fieldShape = RoundedCornerShape(10.dp)
            BasicTextField(
                value = text,
                onValueChange = { text = Profile.clean(it) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Color.White),
                cursorBrush = SolidColor(GoldLight),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .focusRequester(field)
                    .onFocusChanged { fieldFocused = it.isFocused }
                    // A text field keeps the arrow keys for its cursor, which would trap remote users in it.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> runCatching { saveButton.requestFocus() }.isSuccess
                            Key.Enter, Key.NumPadEnter -> {
                                save()
                                true
                            }
                            else -> false
                        }
                    },
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.06f), fieldShape)
                            .border(if (fieldFocused) 2.dp else 1.dp, if (fieldFocused) GoldLight else Color.White.copy(alpha = 0.2f), fieldShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text("Your name", style = TextStyle(fontFamily = UiFont, fontSize = 18.sp, color = Color.White.copy(alpha = 0.35f)))
                        }
                        inner()
                    }
                },
            )

            Row(Modifier.padding(top = 22.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                DialogButton("Skip", primary = false) {
                    Profile.save(context, Profile.name)
                    onDismiss()
                }
                DialogButton("Save", primary = true, Modifier.focusRequester(saveButton), onClick = save)
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) Gold.copy(alpha = 0.18f) else Color.Transparent,
            focusedContainerColor = Gold,
            contentColor = if (primary) GoldLight else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Ink,
        ),
    ) {
        Box(Modifier.padding(horizontal = 22.dp).height(40.dp), contentAlignment = Alignment.Center) {
            Text(label.uppercase(), style = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em))
        }
    }
}
