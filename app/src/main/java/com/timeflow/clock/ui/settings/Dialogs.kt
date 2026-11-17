@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.timeflow.clock.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor

private val Presets = listOf(
    0xFFFFFFFF, 0xFFFFE8C8, 0xFFFFD6A5, 0xFFFFB648, 0xFFFF8A65, 0xFFFF6B8B,
    0xFFE1A6FF, 0xFFB4B8FF, 0xFF9CC8FF, 0xFF7FE0FF, 0xFF8DEBB0, 0xFFD8F28A,
    0xFF000000, 0xFF0E1116, 0xFF141416, 0xFF101826, 0xFF1A0F14, 0xFF0F1A14,
).map { it.toInt() }

/** HSV colour picker with a live swatch and quick presets. */
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val start = remember(initial) { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val argb = AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))

    fun applyPreset(c: Int) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(c, hsv)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = { TextButton(onClick = { onConfirm(argb) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(argb))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                )
                Text(
                    text = String.format("#%06X", argb and 0xFFFFFF),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Hue", style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                    Color.Blue, Color.Magenta, Color.Red,
                                ),
                            ),
                        ),
                )
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("Saturation", style = MaterialTheme.typography.labelLarge)
                Slider(value = sat, onValueChange = { sat = it }, valueRange = 0f..1f)
                Text("Brightness", style = MaterialTheme.typography.labelLarge)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                Text("Presets", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Presets.forEach { c ->
                        ColorDot(
                            color = Color(c),
                            size = 30,
                            modifier = Modifier.clickable { applyPreset(c) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun TimePickDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
fun HowToEnableDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable TIMEFLOW as your screen saver") },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Step("1", "Open Settings → Display & brightness → Screen saver (on some OxygenOS versions: Settings → Display → Screen saver, or search Settings for \"Screen saver\").")
                Step("2", "Turn Screen saver on, then choose \"TIMEFLOW Clock\". The gear icon next to it opens this screen.")
                Step("3", "Set \"When to start\" to While charging (best for tablets), While docked, or Either.")
                Step("4", "Tap \"Start now\" to test. Touch the screen to exit (or double-tap / long-press with touch protection on).")
                Text(
                    "If your OxygenOS build hides the Screen saver page",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "TIMEFLOW is still registered as a real screensaver; OnePlus just hides the menu. " +
                        "Enable it once from a computer with ADB (USB debugging on):",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "adb shell settings put secure screensaver_enabled 1\n" +
                        "adb shell settings put secure screensaver_components com.timeflow.clock/com.timeflow.clock.dream.TimeflowDreamService\n" +
                        "adb shell settings put secure screensaver_activate_on_sleep 1\n" +
                        "adb shell settings put secure screensaver_activate_on_dock 1",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "No computer? Use the \"Start clock\" button — the manual full-screen clock keeps the screen awake with the same design.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

@Composable
private fun Step(number: String, text: String) {
    Text(
        text = "$number.  $text",
        style = MaterialTheme.typography.bodyMedium,
    )
}
