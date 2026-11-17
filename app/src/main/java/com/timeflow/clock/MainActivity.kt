package com.timeflow.clock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.timeflow.clock.data.SettingsRepository
import com.timeflow.clock.ui.settings.HomeScreen
import com.timeflow.clock.ui.theme.TimeflowTheme
import com.timeflow.clock.util.ScreensaverSettings

/** Home: live preview, all customisation, and launch buttons. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository.get(this)
        setContent {
            TimeflowTheme {
                HomeScreen(
                    repository = repository,
                    onStartClock = { startActivity(Intent(this, ClockActivity::class.java)) },
                    onOpenScreensaverSettings = { ScreensaverSettings.open(this) },
                )
            }
        }
    }
}
