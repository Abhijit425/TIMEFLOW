package com.timeflow.clock.ui.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.TimeZone

private fun currentTime(): ZonedDateTime =
    ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)

/**
 * Current wall-clock time from Android system time.
 *
 * - Ticks exactly on the second boundary when [everySecond] is true, otherwise on
 *   the minute boundary (far fewer wake-ups → better battery).
 * - Pauses automatically while the screen/dream is not started.
 * - Reacts immediately to manual time changes, timezone changes (travel, DST rules)
 *   and date changes.
 */
@Composable
fun rememberCurrentTime(everySecond: Boolean): ZonedDateTime {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(currentTime()) }
    var systemChange by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
                    // Drop the cached default so ZoneId.systemDefault() re-reads it.
                    TimeZone.setDefault(null)
                }
                now = currentTime()
                systemChange++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(everySecond, systemChange, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = currentTime()
                val millis = System.currentTimeMillis()
                val wait = if (everySecond) 1_000L - millis % 1_000L else 60_000L - millis % 60_000L
                delay(wait + 8L)
            }
        }
    }
    return now
}

data class BatteryInfo(val percent: Int, val charging: Boolean)

private fun Intent.toBatteryInfo(): BatteryInfo? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(level * 100 / scale, charging)
}

/** Battery level from the sticky system broadcast — no permission required. */
@Composable
fun rememberBatteryInfo(enabled: Boolean): BatteryInfo? {
    val context = LocalContext.current
    var info by remember { mutableStateOf<BatteryInfo?>(null) }
    DisposableEffect(context, enabled) {
        if (!enabled) {
            info = null
            return@DisposableEffect onDispose { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.toBatteryInfo()?.let { info = it }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.toBatteryInfo()?.let { info = it }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return info
}
