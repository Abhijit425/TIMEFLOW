package com.timeflow.clock.dream

import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.timeflow.clock.data.SettingsRepository
import com.timeflow.clock.ui.clock.FullScreenClock
import com.timeflow.clock.util.applyNightBacklight
import com.timeflow.clock.util.enterImmersive

/**
 * The real Android screensaver ("Daydream").
 *
 * Android starts it itself — when charging / docked / both, per
 * Settings → Display → Screen saver → "When to start" — and stops it when the
 * user touches the screen or the device wakes. A DreamService is not an
 * Activity, so we give Compose its own Lifecycle + SavedState owners and drive
 * them from the dream callbacks:
 *
 *   onCreate            → CREATED
 *   onDreamingStarted   → STARTED, RESUMED   (clock ticks)
 *   onDreamingStopped   → STARTED, CREATED   (clock stops ticking, no battery use)
 *   onDestroy           → DESTROYED
 *
 * A fresh service instance is created for every dream session, and settings are
 * re-read from DataStore, so the clock always restores cleanly.
 */
class TimeflowDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Interactive so that we decide what a touch does (needed for touch protection).
        // Without protection, any tap still exits immediately (handled in FullScreenClock).
        isInteractive = true
        isFullscreen = true
        isScreenBright = true

        val repository = SettingsRepository.get(this)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TimeflowDreamService)
            setViewTreeSavedStateRegistryOwner(this@TimeflowDreamService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    settings?.let { s ->
                        FullScreenClock(
                            settings = s,
                            onExitRequest = { finish() },
                            onNightModeChanged = { night ->
                                // Let the system dim the panel too, then lower our window backlight.
                                isScreenBright = !(night && s.nightReduceSystemBrightness)
                                window?.applyNightBacklight(night, s.nightReduceSystemBrightness)
                            },
                        )
                    }
                }
            }
        }

        window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
        setContentView(composeView)
        // The DreamService itself manages screen-on / dim state via isScreenBright.
        window?.enterImmersive()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        super.onDestroy()
    }
}
