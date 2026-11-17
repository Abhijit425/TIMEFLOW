package com.timeflow.clock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.timeflowDataStore: DataStore<Preferences> by preferencesDataStore(name = "timeflow_settings")

/**
 * Single source of truth for settings, shared by the home screen, the manual
 * clock and the DreamService (all run in the same process).
 */
class SettingsRepository private constructor(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<ClockSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun update(transform: (ClockSettings) -> ClockSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext.timeflowDataStore)
                    .also { instance = it }
            }
    }
}

private object Keys {
    val FONT = stringPreferencesKey("font")
    val COLOR = stringPreferencesKey("color")
    val CUSTOM_COLOR = intPreferencesKey("custom_color")
    val BACKGROUND = stringPreferencesKey("background")
    val CUSTOM_BACKGROUND = intPreferencesKey("custom_background")
    val HOUR_FORMAT = stringPreferencesKey("hour_format")
    val DATE_STYLE = stringPreferencesKey("date_style")
    val SHOW_WEEKDAY = booleanPreferencesKey("show_weekday")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
    val SHOW_AM_PM = booleanPreferencesKey("show_am_pm")
    val SHOW_BATTERY = booleanPreferencesKey("show_battery")
    val BLINK_COLON = booleanPreferencesKey("blink_colon")
    val CLOCK_SCALE = floatPreferencesKey("clock_scale")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val ALIGNMENT = stringPreferencesKey("alignment")
    val ORIENTATION = stringPreferencesKey("orientation")
    val SHIFT_INTERVAL = stringPreferencesKey("shift_interval")
    val NIGHT_ENABLED = booleanPreferencesKey("night_enabled")
    val NIGHT_START = intPreferencesKey("night_start")
    val NIGHT_END = intPreferencesKey("night_end")
    val NIGHT_DIM = floatPreferencesKey("night_dim")
    val NIGHT_SYSTEM_BRIGHTNESS = booleanPreferencesKey("night_system_brightness")
    val TOUCH_PROTECTION = booleanPreferencesKey("touch_protection")
}

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    if (this == null) default else enumValues<T>().firstOrNull { it.name == this } ?: default

private fun Preferences.toSettings(): ClockSettings {
    val d = ClockSettings()
    return ClockSettings(
        font = this[Keys.FONT].toEnumOr(d.font),
        color = this[Keys.COLOR].toEnumOr(d.color),
        customColor = this[Keys.CUSTOM_COLOR] ?: d.customColor,
        background = this[Keys.BACKGROUND].toEnumOr(d.background),
        customBackground = this[Keys.CUSTOM_BACKGROUND] ?: d.customBackground,
        hourFormat = this[Keys.HOUR_FORMAT].toEnumOr(d.hourFormat),
        dateStyle = this[Keys.DATE_STYLE].toEnumOr(d.dateStyle),
        showWeekday = this[Keys.SHOW_WEEKDAY] ?: d.showWeekday,
        showDate = this[Keys.SHOW_DATE] ?: d.showDate,
        showSeconds = this[Keys.SHOW_SECONDS] ?: d.showSeconds,
        showAmPm = this[Keys.SHOW_AM_PM] ?: d.showAmPm,
        showBattery = this[Keys.SHOW_BATTERY] ?: d.showBattery,
        blinkColon = this[Keys.BLINK_COLON] ?: d.blinkColon,
        clockScale = (this[Keys.CLOCK_SCALE] ?: d.clockScale).coerceIn(0.4f, 1f),
        brightness = (this[Keys.BRIGHTNESS] ?: d.brightness).coerceIn(0.15f, 1f),
        alignment = this[Keys.ALIGNMENT].toEnumOr(d.alignment),
        orientation = this[Keys.ORIENTATION].toEnumOr(d.orientation),
        shiftInterval = this[Keys.SHIFT_INTERVAL].toEnumOr(d.shiftInterval),
        nightModeEnabled = this[Keys.NIGHT_ENABLED] ?: d.nightModeEnabled,
        nightStartMinutes = (this[Keys.NIGHT_START] ?: d.nightStartMinutes).coerceIn(0, 1439),
        nightEndMinutes = (this[Keys.NIGHT_END] ?: d.nightEndMinutes).coerceIn(0, 1439),
        nightDimLevel = (this[Keys.NIGHT_DIM] ?: d.nightDimLevel).coerceIn(0.1f, 1f),
        nightReduceSystemBrightness = this[Keys.NIGHT_SYSTEM_BRIGHTNESS] ?: d.nightReduceSystemBrightness,
        touchProtection = this[Keys.TOUCH_PROTECTION] ?: d.touchProtection,
    )
}

private fun MutablePreferences.write(s: ClockSettings) {
    this[Keys.FONT] = s.font.name
    this[Keys.COLOR] = s.color.name
    this[Keys.CUSTOM_COLOR] = s.customColor
    this[Keys.BACKGROUND] = s.background.name
    this[Keys.CUSTOM_BACKGROUND] = s.customBackground
    this[Keys.HOUR_FORMAT] = s.hourFormat.name
    this[Keys.DATE_STYLE] = s.dateStyle.name
    this[Keys.SHOW_WEEKDAY] = s.showWeekday
    this[Keys.SHOW_DATE] = s.showDate
    this[Keys.SHOW_SECONDS] = s.showSeconds
    this[Keys.SHOW_AM_PM] = s.showAmPm
    this[Keys.SHOW_BATTERY] = s.showBattery
    this[Keys.BLINK_COLON] = s.blinkColon
    this[Keys.CLOCK_SCALE] = s.clockScale
    this[Keys.BRIGHTNESS] = s.brightness
    this[Keys.ALIGNMENT] = s.alignment.name
    this[Keys.ORIENTATION] = s.orientation.name
    this[Keys.SHIFT_INTERVAL] = s.shiftInterval.name
    this[Keys.NIGHT_ENABLED] = s.nightModeEnabled
    this[Keys.NIGHT_START] = s.nightStartMinutes
    this[Keys.NIGHT_END] = s.nightEndMinutes
    this[Keys.NIGHT_DIM] = s.nightDimLevel
    this[Keys.NIGHT_SYSTEM_BRIGHTNESS] = s.nightReduceSystemBrightness
    this[Keys.TOUCH_PROTECTION] = s.touchProtection
}
