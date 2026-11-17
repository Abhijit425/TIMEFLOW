package com.timeflow.clock.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** True full-screen: hides status + navigation bars; a swipe from the edge shows them briefly. */
fun Window.enterImmersive() {
    WindowCompat.setDecorFitsSystemWindows(this, false)
    WindowCompat.getInsetsController(this, decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val lp = attributes
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        attributes = lp
    }
}

/**
 * Lowers the panel backlight for this window only (no WRITE_SETTINGS permission
 * needed). Restores the user's normal brightness as soon as night mode ends or
 * the window closes.
 */
fun Window.applyNightBacklight(night: Boolean, reduce: Boolean) {
    val lp = attributes
    val target = if (night && reduce) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    if (lp.screenBrightness != target) {
        lp.screenBrightness = target
        attributes = lp
    }
}

object ScreensaverSettings {
    /**
     * Opens Android's Screen saver page. Some OxygenOS builds hide it; then we fall
     * back to Display settings and tell the user.
     */
    fun open(context: Context) {
        val attempts = listOf(
            Intent(Settings.ACTION_DREAM_SETTINGS) to null,
            Intent(Settings.ACTION_DISPLAY_SETTINGS) to
                "Screen saver page not exposed on this OxygenOS build — opened Display settings. See \"How to enable\".",
            Intent(Settings.ACTION_SETTINGS) to
                "Opened Settings. See \"How to enable\" for the ADB method.",
        )
        for ((intent, message) in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return
            } catch (e: ActivityNotFoundException) {
                // try next
            } catch (e: SecurityException) {
                // try next
            }
        }
    }
}
