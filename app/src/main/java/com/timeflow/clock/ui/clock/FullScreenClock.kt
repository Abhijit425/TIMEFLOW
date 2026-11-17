package com.timeflow.clock.ui.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timeflow.clock.data.ClockSettings
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * OLED burn-in protection: every [intervalMinutes] the clock glides (over 4 s,
 * eased) to a new random position within ±[maxShift]. Slow enough to be invisible
 * from across the room, frequent enough that no pixel stays lit in one place.
 */
@Composable
fun rememberBurnInOffset(intervalMinutes: Int, maxShift: Dp): DpOffset {
    var target by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    LaunchedEffect(intervalMinutes, maxShift) {
        if (intervalMinutes <= 0) {
            target = DpOffset(0.dp, 0.dp)
            return@LaunchedEffect
        }
        while (true) {
            delay(intervalMinutes * 60_000L)
            target = DpOffset(
                x = maxShift * (Random.nextFloat() * 2f - 1f),
                y = maxShift * (Random.nextFloat() * 2f - 1f),
            )
        }
    }
    val spec = tween<Dp>(durationMillis = 4_000, easing = FastOutSlowInEasing)
    val x by animateDpAsState(target.x, spec, label = "burnInX")
    val y by animateDpAsState(target.y, spec, label = "burnInY")
    return DpOffset(x, y)
}

/**
 * Full-screen clock shared by the manual clock (ClockActivity) and the system
 * screensaver (TimeflowDreamService).
 *
 * @param onExitRequest called on tap (or long-press / double-tap when touch protection is on)
 * @param onNightModeChanged lets the host window lower the backlight at night
 */
@Composable
fun FullScreenClock(
    settings: ClockSettings,
    onExitRequest: () -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
) {
    val now = rememberCurrentTime(everySecond = settings.showSeconds || settings.blinkColon)
    val battery = rememberBatteryInfo(enabled = settings.showBattery)
    val night = settings.isNightAt(now.toLocalTime())
    val currentOnNight by rememberUpdatedState(onNightModeChanged)
    LaunchedEffect(night, settings.nightReduceSystemBrightness) { currentOnNight(night) }

    val offset = rememberBurnInOffset(settings.shiftInterval.minutes, maxShift = 28.dp)

    // Gentle fade-in every time the clock (re)appears.
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(durationMillis = 1_400)) }

    var showHint by remember { mutableStateOf(false) }
    LaunchedEffect(showHint) {
        if (showHint) {
            delay(2_500)
            showHint = false
        }
    }

    val exit by rememberUpdatedState(onExitRequest)
    val isProtected = settings.touchProtection

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isProtected) {
                if (isProtected) {
                    detectTapGestures(
                        onTap = { showHint = true },
                        onDoubleTap = { exit() },
                        onLongPress = { exit() },
                    )
                } else {
                    detectTapGestures(onTap = { exit() })
                }
            },
    ) {
        ClockFace(
            settings = settings,
            now = now,
            battery = battery,
            nightActive = night,
            contentOffset = offset,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value },
        )

        AnimatedVisibility(
            visible = showHint,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        ) {
            Text(
                text = "Touch protection is on · double-tap or long-press to exit",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
