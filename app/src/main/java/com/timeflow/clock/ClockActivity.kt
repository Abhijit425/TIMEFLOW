package com.timeflow.clock

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timeflow.clock.data.OrientationMode
import com.timeflow.clock.data.SettingsRepository
import com.timeflow.clock.ui.clock.FullScreenClock
import com.timeflow.clock.util.applyNightBacklight
import com.timeflow.clock.util.enterImmersive

/**
 * Manual full-screen clock mode ("Start clock" button). Keeps the screen awake
 * while visible, hides system bars, and handles rotation without restarting
 * (configChanges in the manifest), so the clock never blinks out.
 */
class ClockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.enterImmersive()

        val repository = SettingsRepository.get(this)
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                settings?.let { s ->
                    LaunchedEffect(s.orientation) {
                        requestedOrientation = when (s.orientation) {
                            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                    FullScreenClock(
                        settings = s,
                        onExitRequest = { finish() },
                        onNightModeChanged = { night ->
                            window.applyNightBacklight(night, s.nightReduceSystemBrightness)
                        },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-hide bars after dialogs, notifications shade, etc.
        if (hasFocus) window.enterImmersive()
    }
}
