package com.timeflow.clock.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timeflow.clock.data.BackgroundStyle
import com.timeflow.clock.data.ClockAlignment
import com.timeflow.clock.data.ClockColor
import com.timeflow.clock.data.ClockFont
import com.timeflow.clock.data.ClockSettings
import com.timeflow.clock.data.DateStyle
import com.timeflow.clock.data.HourFormat
import com.timeflow.clock.data.OrientationMode
import com.timeflow.clock.data.SettingsRepository
import com.timeflow.clock.data.ShiftInterval
import com.timeflow.clock.ui.clock.ClockFace
import com.timeflow.clock.ui.clock.NightWarmWhite
import com.timeflow.clock.ui.clock.rememberBatteryInfo
import com.timeflow.clock.ui.clock.rememberCurrentTime
import com.timeflow.clock.ui.theme.Hairline
import com.timeflow.clock.ui.theme.Ink
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private enum class HomeDialog { CUSTOM_COLOR, CUSTOM_BACKGROUND, NIGHT_START, NIGHT_END, HOW_TO }

private typealias Update = ((ClockSettings) -> ClockSettings) -> Unit

@Composable
fun HomeScreen(
    repository: SettingsRepository,
    onStartClock: () -> Unit,
    onOpenScreensaverSettings: () -> Unit,
) {
    val loaded by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = loaded
    if (settings == null) {
        Box(Modifier.fillMaxSize().background(Ink))
        return
    }
    val scope = rememberCoroutineScope()
    val update: Update = { transform -> scope.launch { repository.update(transform) } }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val openDialog: (HomeDialog) -> Unit = { dialog = it }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            // Tablet landscape: sticky preview on the left, settings scroll on the right.
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Header()
                    PreviewCard(settings)
                    ActionButtons(onStartClock, onOpenScreensaverSettings) { openDialog(HomeDialog.HOW_TO) }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                ) {
                    settingsSections(settings, update, openDialog)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            ) {
                item(key = "header") { Header() }
                item(key = "preview") { PreviewCard(settings) }
                item(key = "actions") {
                    ActionButtons(onStartClock, onOpenScreensaverSettings) { openDialog(HomeDialog.HOW_TO) }
                }
                settingsSections(settings, update, openDialog)
            }
        }
    }

    when (dialog) {
        HomeDialog.CUSTOM_COLOR -> ColorPickerDialog(
            title = "Clock colour",
            initial = settings.customColor,
            onDismiss = { dialog = null },
            onConfirm = { c ->
                update { it.copy(customColor = c, color = ClockColor.CUSTOM) }
                dialog = null
            },
        )
        HomeDialog.CUSTOM_BACKGROUND -> ColorPickerDialog(
            title = "Background colour",
            initial = settings.customBackground,
            onDismiss = { dialog = null },
            onConfirm = { c ->
                update { it.copy(customBackground = c, background = BackgroundStyle.CUSTOM) }
                dialog = null
            },
        )
        HomeDialog.NIGHT_START -> TimePickDialog(
            title = "Night mode starts",
            initialMinutes = settings.nightStartMinutes,
            onDismiss = { dialog = null },
            onConfirm = { m ->
                update { it.copy(nightStartMinutes = m) }
                dialog = null
            },
        )
        HomeDialog.NIGHT_END -> TimePickDialog(
            title = "Night mode ends",
            initialMinutes = settings.nightEndMinutes,
            onDismiss = { dialog = null },
            onConfirm = { m ->
                update { it.copy(nightEndMinutes = m) }
                dialog = null
            },
        )
        HomeDialog.HOW_TO -> HowToEnableDialog(
            onDismiss = { dialog = null },
            onOpenSettings = {
                dialog = null
                onOpenScreensaverSettings()
            },
        )
        null -> Unit
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "TIMEFLOW",
            fontSize = 34.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 10.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Digital clock screensaver",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewCard(settings: ClockSettings) {
    val now = rememberCurrentTime(everySecond = settings.showSeconds || settings.blinkColon)
    val battery = rememberBatteryInfo(enabled = settings.showBattery)
    val night = settings.isNightAt(now.toLocalTime())
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(26.dp))
                .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(26.dp)),
        ) {
            ClockFace(
                settings = settings,
                now = now,
                battery = battery,
                nightActive = night,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("LIVE PREVIEW", MaterialTheme.colorScheme.onSurfaceVariant)
            if (night) Pill("NIGHT MODE ACTIVE", NightWarmWhite)
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionButtons(
    onStartClock: () -> Unit,
    onOpenScreensaverSettings: () -> Unit,
    onHowTo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onStartClock,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Start clock", fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onOpenScreensaverSettings,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Screen saver settings") }
            TextButton(
                onClick = onHowTo,
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("How to enable") }
        }
    }
}

private fun formatMinutes(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun LazyListScope.settingsSections(
    s: ClockSettings,
    update: Update,
    openDialog: (HomeDialog) -> Unit,
) {
    item(key = "appearance") {
        SectionCard(title = "Clock appearance") {
            ChoiceChips(
                options = ClockFont.entries,
                selected = s.font,
                label = { it.label },
                onSelect = { f -> update { it.copy(font = f) } },
            )
        }
    }

    item(key = "colours") {
        SectionCard(title = "Colour", subtitle = "Night mode switches to dim warm white automatically.") {
            ChoiceChips(
                options = ClockColor.entries,
                selected = s.color,
                label = { it.label },
                onSelect = { c ->
                    if (c == ClockColor.CUSTOM) openDialog(HomeDialog.CUSTOM_COLOR)
                    else update { it.copy(color = c) }
                },
                swatch = { c -> Color(if (c == ClockColor.CUSTOM) s.customColor else c.argb) },
            )
        }
    }

    item(key = "background") {
        SectionCard(title = "Background") {
            ChoiceChips(
                options = BackgroundStyle.entries,
                selected = s.background,
                label = { it.label },
                onSelect = { b ->
                    if (b == BackgroundStyle.CUSTOM) openDialog(HomeDialog.CUSTOM_BACKGROUND)
                    else update { it.copy(background = b) }
                },
                swatch = { b ->
                    when (b) {
                        BackgroundStyle.PURE_BLACK -> Color.Black
                        BackgroundStyle.DARK_GREY -> Color(0xFF141416)
                        BackgroundStyle.CUSTOM -> Color(s.customBackground)
                        BackgroundStyle.GRADIENT -> Color(0xFF0C1220)
                    }
                },
            )
            if (s.background != BackgroundStyle.PURE_BLACK) {
                Text(
                    "Tip: pure black keeps OLED pixels fully off — best for battery and burn-in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    item(key = "time") {
        SectionCard(title = "Time & date") {
            FieldLabel("Time format")
            ChoiceChips(
                options = HourFormat.entries,
                selected = s.hourFormat,
                label = { it.label },
                onSelect = { h -> update { it.copy(hourFormat = h) } },
            )
            FieldLabel("Date format")
            ChoiceChips(
                options = DateStyle.entries,
                selected = s.dateStyle,
                label = { it.label },
                onSelect = { d -> update { it.copy(dateStyle = d) } },
            )
            ToggleRow("Show weekday", s.showWeekday, { v -> update { it.copy(showWeekday = v) } })
            ToggleRow("Show date", s.showDate, { v -> update { it.copy(showDate = v) } })
            ToggleRow(
                "Show seconds",
                s.showSeconds,
                { v -> update { it.copy(showSeconds = v) } },
                subtitle = "Updates every second; off saves a little battery",
            )
            ToggleRow("Blinking colon", s.blinkColon, { v -> update { it.copy(blinkColon = v) } })
            ToggleRow(
                "Show AM / PM",
                s.showAmPm,
                { v -> update { it.copy(showAmPm = v) } },
                subtitle = "12-hour format only",
            )
            ToggleRow("Show battery percentage", s.showBattery, { v -> update { it.copy(showBattery = v) } })
        }
    }

    item(key = "layout") {
        SectionCard(title = "Size & layout") {
            SliderRow(
                title = "Clock size",
                value = s.clockScale,
                range = 0.4f..1f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onCommit = { v -> update { it.copy(clockScale = v) } },
            )
            SliderRow(
                title = "Brightness",
                value = s.brightness,
                range = 0.15f..1f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onCommit = { v -> update { it.copy(brightness = v) } },
            )
            FieldLabel("Alignment")
            ChoiceChips(
                options = ClockAlignment.entries,
                selected = s.alignment,
                label = { it.label },
                onSelect = { a -> update { it.copy(alignment = a) } },
            )
            FieldLabel("Orientation (manual clock)")
            ChoiceChips(
                options = OrientationMode.entries,
                selected = s.orientation,
                label = { it.label },
                onSelect = { o -> update { it.copy(orientation = o) } },
            )
        }
    }

    item(key = "burnin") {
        SectionCard(
            title = "Burn-in protection",
            subtitle = "The clock glides a few pixels to a new spot at this interval so no OLED pixel stays lit in one place.",
        ) {
            ChoiceChips(
                options = ShiftInterval.entries,
                selected = s.shiftInterval,
                label = { it.label },
                onSelect = { i -> update { it.copy(shiftInterval = i) } },
            )
        }
    }

    item(key = "night") {
        SectionCard(title = "Night mode", subtitle = "Dim warm-white clock on pure black during the hours you choose.") {
            ToggleRow("Automatic night mode", s.nightModeEnabled, { v -> update { it.copy(nightModeEnabled = v) } })
            if (s.nightModeEnabled) {
                ValueRow("Starts", formatMinutes(s.nightStartMinutes)) { openDialog(HomeDialog.NIGHT_START) }
                ValueRow("Ends", formatMinutes(s.nightEndMinutes)) { openDialog(HomeDialog.NIGHT_END) }
                SliderRow(
                    title = "Night dimming",
                    value = s.nightDimLevel,
                    range = 0.1f..1f,
                    valueLabel = { "${(it * 100).roundToInt()}%" },
                    onCommit = { v -> update { it.copy(nightDimLevel = v) } },
                )
                ToggleRow(
                    "Lower screen backlight at night",
                    s.nightReduceSystemBrightness,
                    { v -> update { it.copy(nightReduceSystemBrightness = v) } },
                    subtitle = "Only while the clock is showing; normal brightness returns afterwards",
                )
            }
        }
    }

    item(key = "touch") {
        SectionCard(title = "Interaction") {
            ToggleRow(
                "Touch protection",
                s.touchProtection,
                { v -> update { it.copy(touchProtection = v) } },
                subtitle = "Ignore accidental taps — double-tap or long-press to exit",
            )
        }
    }

    item(key = "about") {
        SectionCard(title = "About") {
            Text(
                "TIMEFLOW is fully offline: no internet permission, no accounts, no ads, no API keys. " +
                    "Weather is not shown because Android has no offline weather source — every weather feed " +
                    "needs an online service, so it is left out rather than faked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}
