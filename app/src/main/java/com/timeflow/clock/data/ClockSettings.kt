package com.timeflow.clock.data

import java.time.LocalTime

enum class ClockFont(val label: String) {
    MINIMAL("Minimalist digital"),
    MODERN("Modern"),
    THIN("Thin"),
    BOLD("Bold"),
    SERIF("Elegant serif"),
}

enum class ClockColor(val label: String, val argb: Int) {
    PURE_WHITE("Pure white", 0xFFFFFFFF.toInt()),
    WARM_WHITE("Warm white", 0xFFFFE8C8.toInt()),
    SOFT_BLUE("Soft blue", 0xFF9CC8FF.toInt()),
    GREEN("Green", 0xFF8DEBB0.toInt()),
    AMBER("Amber", 0xFFFFB648.toInt()),
    CUSTOM("Custom", 0),
}

enum class BackgroundStyle(val label: String) {
    PURE_BLACK("Pure black"),
    DARK_GREY("Dark grey"),
    CUSTOM("Custom colour"),
    GRADIENT("Subtle gradient"),
}

enum class HourFormat(val label: String) {
    SYSTEM("Follow system"),
    H12("12-hour"),
    H24("24-hour"),
}

enum class DateStyle(val label: String, val pattern: String) {
    DAY_MONTH_YEAR("17 November 2026", "d MMMM yyyy"),
    MONTH_DAY_YEAR("November 17, 2026", "MMMM d, yyyy"),
    SHORT("Tue, 17 Nov", "EEE, d MMM"),
    NUMERIC("17/11/2026", "dd/MM/yyyy"),
    ISO("2026-11-17", "yyyy-MM-dd"),
}

enum class ClockAlignment(val label: String) {
    CENTER("Center"),
    START("Left"),
    END("Right"),
}

enum class OrientationMode(val label: String) {
    AUTO("Auto-rotate"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
}

enum class ShiftInterval(val label: String, val minutes: Int) {
    OFF("Off", 0),
    ONE("1 min", 1),
    TWO("2 min", 2),
    FIVE("5 min", 5),
    TEN("10 min", 10),
}

/** Every user preference. Defaults are the out-of-the-box TIMEFLOW look. */
data class ClockSettings(
    val font: ClockFont = ClockFont.THIN,
    val color: ClockColor = ClockColor.PURE_WHITE,
    val customColor: Int = 0xFFE0F0FF.toInt(),
    val background: BackgroundStyle = BackgroundStyle.PURE_BLACK,
    val customBackground: Int = 0xFF0E1116.toInt(),
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val dateStyle: DateStyle = DateStyle.DAY_MONTH_YEAR,
    val showWeekday: Boolean = true,
    val showDate: Boolean = true,
    val showSeconds: Boolean = false,
    val showAmPm: Boolean = true,
    val showBattery: Boolean = true,
    val blinkColon: Boolean = false,
    /** 0.4 – 1.0 of the largest size that fits the screen. */
    val clockScale: Float = 1f,
    /** 0.15 – 1.0 content brightness (dims the pixels themselves — best for OLED). */
    val brightness: Float = 0.9f,
    val alignment: ClockAlignment = ClockAlignment.CENTER,
    val orientation: OrientationMode = OrientationMode.AUTO,
    val shiftInterval: ShiftInterval = ShiftInterval.TWO,
    val nightModeEnabled: Boolean = true,
    /** Minutes after midnight. */
    val nightStartMinutes: Int = 22 * 60,
    val nightEndMinutes: Int = 7 * 60,
    /** Extra dimming multiplier applied at night (0.1 – 1.0). */
    val nightDimLevel: Float = 0.35f,
    val nightReduceSystemBrightness: Boolean = true,
    val touchProtection: Boolean = false,
) {
    fun timeColorArgb(): Int = if (color == ClockColor.CUSTOM) customColor else color.argb

    fun isNightAt(time: LocalTime): Boolean {
        if (!nightModeEnabled || nightStartMinutes == nightEndMinutes) return false
        val m = time.hour * 60 + time.minute
        return if (nightStartMinutes < nightEndMinutes) {
            m >= nightStartMinutes && m < nightEndMinutes
        } else {
            // Window crosses midnight, e.g. 22:00 → 07:00
            m >= nightStartMinutes || m < nightEndMinutes
        }
    }
}
