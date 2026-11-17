package com.timeflow.clock.ui.clock

import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.timeflow.clock.data.BackgroundStyle
import com.timeflow.clock.data.ClockAlignment
import com.timeflow.clock.data.ClockFont
import com.timeflow.clock.data.ClockSettings
import com.timeflow.clock.data.HourFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/** Dim warm white used for all text while night mode is active. */
val NightWarmWhite = Color(0xFFFFC58A)

private val CondensedFamily: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

fun ClockFont.fontFamily(): FontFamily = when (this) {
    ClockFont.MINIMAL -> FontFamily.SansSerif
    ClockFont.MODERN -> CondensedFamily
    ClockFont.THIN -> FontFamily.SansSerif
    ClockFont.BOLD -> FontFamily.SansSerif
    ClockFont.SERIF -> FontFamily.Serif
}

fun ClockFont.weight(): FontWeight = when (this) {
    ClockFont.MINIMAL -> FontWeight.Light
    ClockFont.MODERN -> FontWeight.Normal
    ClockFont.THIN -> FontWeight.Thin
    ClockFont.BOLD -> FontWeight.Black
    ClockFont.SERIF -> FontWeight.Normal
}

/** Approximate advance width of one tabular digit, in em. Used to size the clock to the screen. */
private fun ClockFont.digitWidth(): Float = when (this) {
    ClockFont.MINIMAL -> 0.58f
    ClockFont.MODERN -> 0.52f
    ClockFont.THIN -> 0.57f
    ClockFont.BOLD -> 0.66f
    ClockFont.SERIF -> 0.62f
}

fun backgroundBrush(settings: ClockSettings, nightActive: Boolean): Brush = when {
    nightActive -> SolidColor(Color.Black)
    else -> when (settings.background) {
        BackgroundStyle.PURE_BLACK -> SolidColor(Color.Black)
        BackgroundStyle.DARK_GREY -> SolidColor(Color(0xFF141416))
        BackgroundStyle.CUSTOM -> SolidColor(Color(settings.customBackground))
        BackgroundStyle.GRADIENT -> Brush.verticalGradient(
            listOf(Color(0xFF0C1220), Color(0xFF050608), Color(0xFF140C18)),
        )
    }
}

/**
 * The clock itself. Pure presentation: it fills whatever space it is given and
 * scales the typography so the time is as large as possible without clipping,
 * in portrait or landscape, on a 7" phone preview or a 13" tablet.
 */
@Composable
fun ClockFace(
    settings: ClockSettings,
    now: ZonedDateTime,
    battery: BatteryInfo?,
    nightActive: Boolean,
    modifier: Modifier = Modifier,
    contentOffset: DpOffset = DpOffset.Zero,
    animateDigits: Boolean = true,
) {
    val context = LocalContext.current
    val use24h = when (settings.hourFormat) {
        HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        HourFormat.H24 -> true
        HourFormat.H12 -> false
    }
    val locale = Locale.getDefault()
    val weekdayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    val dateFormatter = remember(settings.dateStyle, locale) {
        DateTimeFormatter.ofPattern(settings.dateStyle.pattern, locale)
    }

    val hourValue = if (use24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val hours = String.format(Locale.ROOT, "%02d", hourValue)
    val minutes = String.format(Locale.ROOT, "%02d", now.minute)
    val seconds = if (settings.showSeconds) String.format(Locale.ROOT, "%02d", now.second) else null
    val amPm = if (use24h || !settings.showAmPm) null else if (now.hour < 12) "AM" else "PM"

    val baseColor = if (nightActive) NightWarmWhite else Color(settings.timeColorArgb())
    val contentAlpha = (settings.brightness * if (nightActive) settings.nightDimLevel else 1f)
        .coerceIn(0.05f, 1f)
    val colonAlpha by animateFloatAsState(
        targetValue = if (settings.blinkColon && now.second % 2 == 1) 0.12f else 1f,
        animationSpec = tween(durationMillis = 450),
        label = "colon",
    )

    val boxAlignment = when (settings.alignment) {
        ClockAlignment.CENTER -> Alignment.Center
        ClockAlignment.START -> Alignment.CenterStart
        ClockAlignment.END -> Alignment.CenterEnd
    }

    BoxWithConstraints(
        modifier = modifier.background(backgroundBrush(settings, nightActive)),
        contentAlignment = boxAlignment,
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else widthPx * 0.6f

        // Width of "HH:MM" plus the small trailing seconds / AM-PM column, in em.
        val digit = settings.font.digitWidth()
        val hasTrailing = seconds != null || amPm != null
        val units = digit * 4f + 0.34f + if (hasTrailing) 0.1f + digit * 0.72f else 0f
        val sidePadPx = if (settings.alignment == ClockAlignment.CENTER) 0f else widthPx * 0.06f
        val emPx = (min((widthPx - 2 * sidePadPx) * 0.88f / units, heightPx * 0.46f) * settings.clockScale)
            .coerceAtLeast(1f)
        val emSp = with(density) { emPx.toSp() }
        val emDp: Dp = with(density) { emPx.toDp() }
        val sidePad: Dp = with(density) { sidePadPx.toDp() }

        val textAlign = when (settings.alignment) {
            ClockAlignment.CENTER -> TextAlign.Center
            ClockAlignment.START -> TextAlign.Start
            ClockAlignment.END -> TextAlign.End
        }
        val timeStyle = TextStyle(
            fontFamily = settings.font.fontFamily(),
            fontWeight = settings.font.weight(),
            fontSize = emSp,
            lineHeight = emSp,
            color = baseColor,
            fontFeatureSettings = "tnum",
            textAlign = textAlign,
        )
        val secondaryFamily = if (settings.font == ClockFont.SERIF) FontFamily.Serif else FontFamily.SansSerif
        val secondaryWeight = if (settings.font == ClockFont.BOLD) FontWeight.Medium else FontWeight.Light
        val secondaryColor = baseColor.copy(alpha = 0.72f)

        Column(
            modifier = Modifier
                .offset(x = contentOffset.x, y = contentOffset.y)
                .padding(horizontal = sidePad)
                .graphicsLayer { alpha = contentAlpha },
            horizontalAlignment = when (settings.alignment) {
                ClockAlignment.CENTER -> Alignment.CenterHorizontally
                ClockAlignment.START -> Alignment.Start
                ClockAlignment.END -> Alignment.End
            },
        ) {
            if (settings.showWeekday) {
                Text(
                    text = now.format(weekdayFormatter).uppercase(locale),
                    style = TextStyle(
                        fontFamily = secondaryFamily,
                        fontWeight = secondaryWeight,
                        fontSize = emSp * 0.115f,
                        lineHeight = emSp * 0.14f,
                        letterSpacing = 0.34.em,
                        color = secondaryColor,
                        textAlign = textAlign,
                    ),
                    maxLines = 1,
                )
                Spacer(Modifier.height(emDp * 0.03f))
            }

            TimeRow(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                amPm = amPm,
                style = timeStyle,
                emDp = emDp,
                colonAlpha = colonAlpha,
                animate = animateDigits,
            )

            if (settings.showDate) {
                Spacer(Modifier.height(emDp * 0.03f))
                Text(
                    text = now.format(dateFormatter).uppercase(locale),
                    style = TextStyle(
                        fontFamily = secondaryFamily,
                        fontWeight = secondaryWeight,
                        fontSize = emSp * 0.12f,
                        lineHeight = emSp * 0.15f,
                        letterSpacing = 0.16.em,
                        color = baseColor.copy(alpha = 0.88f),
                        textAlign = textAlign,
                    ),
                    maxLines = 1,
                )
            }

            if (settings.showBattery && battery != null) {
                Spacer(Modifier.height(emDp * 0.09f))
                Text(
                    text = if (battery.charging) "${battery.percent}%  ·  CHARGING" else "${battery.percent}%",
                    style = TextStyle(
                        fontFamily = secondaryFamily,
                        fontWeight = secondaryWeight,
                        fontSize = emSp * 0.075f,
                        lineHeight = emSp * 0.1f,
                        letterSpacing = 0.2.em,
                        color = baseColor.copy(alpha = 0.55f),
                        fontFeatureSettings = "tnum",
                        textAlign = textAlign,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TimeRow(
    hours: String,
    minutes: String,
    seconds: String?,
    amPm: String?,
    style: TextStyle,
    emDp: Dp,
    colonAlpha: Float,
    animate: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DigitGroup(hours, style, animate)
        Text(
            text = ":",
            style = style,
            maxLines = 1,
            modifier = Modifier
                .alpha(colonAlpha)
                .padding(horizontal = emDp * 0.01f),
        )
        DigitGroup(minutes, style, animate)

        if (seconds != null || amPm != null) {
            // Small trailing column: AM/PM aligned to the top of the digits,
            // seconds aligned to the baseline area — like a premium bedside clock.
            Column(
                modifier = Modifier
                    .padding(start = emDp * 0.1f)
                    .height(emDp * 0.72f),
                verticalArrangement = when {
                    seconds != null && amPm != null -> Arrangement.SpaceBetween
                    amPm != null -> Arrangement.Top
                    else -> Arrangement.Bottom
                },
            ) {
                if (amPm != null) {
                    Text(
                        text = amPm,
                        style = style.copy(
                            fontSize = style.fontSize * 0.2f,
                            lineHeight = style.fontSize * 0.22f,
                            letterSpacing = 0.12.em,
                            color = style.color.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start,
                        ),
                        maxLines = 1,
                    )
                }
                if (seconds != null) {
                    Text(
                        text = seconds,
                        style = style.copy(
                            fontSize = style.fontSize * 0.34f,
                            lineHeight = style.fontSize * 0.36f,
                            color = style.color.copy(alpha = 0.7f),
                            textAlign = TextAlign.Start,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitGroup(value: String, style: TextStyle, animate: Boolean) {
    if (!animate) {
        Text(text = value, style = style, maxLines = 1, softWrap = false)
        return
    }
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 8 }) togetherWith
                (fadeOut(tween(450)) + slideOutVertically(tween(450)) { -it / 8 })
        },
        label = "digits",
    ) { v ->
        Text(text = v, style = style, maxLines = 1, softWrap = false)
    }
}

/** Default zero offset helper for callers that don't shift the clock. */
val NoOffset = DpOffset(0.dp, 0.dp)
