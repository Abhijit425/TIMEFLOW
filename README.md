# TIMEFLOW — Digital Clock Screensaver

A premium full-screen digital clock for OnePlus tablets (OxygenOS), built as a **genuine Android screensaver** (`DreamService`) plus a manual full-screen clock mode.

- Kotlin · Jetpack Compose · Material 3 · DataStore
- minSdk 26 · targetSdk/compileSdk 36 · AGP 8.13.0 · Gradle 8.14.3 · Kotlin 2.2.20
- **No permissions**, no internet, no API keys, no ads, no backend. Works fully offline.

---

## Features

| Area | What you get |
|---|---|
| Clock | Huge auto-fitted time, weekday, full date, optional seconds, AM/PM, battery %, blinking colon. 12h / 24h / follow system. 5 date formats. |
| Time accuracy | Android system time; ticks exactly on the second (or minute when seconds/blink are off, to save battery); reacts instantly to timezone, DST and manual time changes. |
| Screensaver | Real `DreamService` registered with `BIND_DREAM_SERVICE`, listed in Android's Screen saver settings as **TIMEFLOW Clock**, with a settings gear that opens the app. Starts/stops with Android's screensaver lifecycle (charging / docked). |
| Manual mode | **Start clock** button: immersive full screen, keeps the screen awake, rotation without restart, orientation lock option. |
| Burn-in protection | Clock glides (4 s, eased) to a new random spot within ±28 dp every 1/2/5/10 min (or off). |
| Night mode | Scheduled window (default 22:00–07:00): dim warm-white text on pure black, extra dimming level, optional backlight reduction for the clock window only. |
| Customisation | Typography: Minimalist, Modern, Thin, Bold, Elegant serif · Colours: pure white, warm white, soft blue, green, amber, custom HSV picker · Backgrounds: pure black, dark grey, custom, subtle gradient · Size, brightness, alignment, orientation · Touch protection. |
| Persistence | Everything saved in DataStore and restored on every launch / every screensaver start. |
| Tablet UI | Two-pane home screen on wide displays (live preview left, settings right), single column in portrait. |

**Weather:** deliberately not included. Android has no offline weather source; every weather feed needs an online service or API key, which this project rules out. Rather than fake it, TIMEFLOW leaves it out.

---

## Project structure

```
TIMEFLOW/
├─ settings.gradle.kts, build.gradle.kts, gradle.properties
├─ gradle/libs.versions.toml          ← all versions in one place
├─ gradle/wrapper/…, gradlew, gradlew.bat
├─ .github/workflows/build.yml        ← optional cloud build (GitHub Actions)
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ res/xml/timeflow_dream.xml   ← screensaver metadata (settings activity)
      └─ java/com/timeflow/clock/
         ├─ MainActivity.kt           ← home: preview + settings + buttons
         ├─ ClockActivity.kt          ← manual full-screen clock
         ├─ dream/TimeflowDreamService.kt  ← the system screensaver
         ├─ data/ClockSettings.kt, SettingsRepository.kt  ← model + DataStore
         ├─ ui/clock/ClockFace.kt     ← adaptive clock rendering
         ├─ ui/clock/FullScreenClock.kt ← burn-in shift, night mode, touch handling
         ├─ ui/clock/TimeSource.kt    ← second-accurate ticker, battery
         ├─ ui/settings/…             ← home screen, chips, sliders, dialogs
         ├─ ui/theme/Theme.kt
         └─ util/WindowExt.kt         ← immersive mode, backlight, settings intents
```

---

## Build the APK

### Option A — Android Studio (Windows/macOS/Linux)

1. Install **Android Studio** (Narwhal 2025.1.3 or newer; any current stable release works).
2. Unzip `TIMEFLOW.zip`, e.g. to `C:\TIMEFLOW`.
3. Android Studio → **File → Open** → select the `TIMEFLOW` folder → **Trust project**.
4. Wait for Gradle sync. If it asks to install **Android SDK Platform 36**, click *Install*.
5. **Debug APK:** *Build → Generate App Bundles or APKs → Generate APKs*.
   Output: `app\build\outputs\apk\debug\app-debug.apk`
6. **Release APK:** *Build → Generate Signed App Bundle or APK → APK → create a new keystore → release*.
   Output: `app\release\app-release.apk` (or `app\build\outputs\apk\release\`).

### Option B — command line

```bash
# Windows: use gradlew.bat instead of ./gradlew
./gradlew assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # → app/build/outputs/apk/release/app-release.apk
```

Needs JDK 17+ (Android Studio's bundled JBR is fine: set `JAVA_HOME` to it) and the Android SDK (`local.properties` with `sdk.dir=…`, created automatically by Android Studio).

**Release signing.** Without a keystore, `assembleRelease` signs with the debug key so the APK still installs. For a proper release key:

```bash
keytool -genkeypair -v -keystore timeflow.jks -alias timeflow -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` in the project root (it is git-ignored):

```
storeFile=timeflow.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=timeflow
keyPassword=YOUR_KEY_PASSWORD
```

### Option C — GitHub Actions (no local SDK needed)

Push the folder to a GitHub repository. The included workflow builds both APKs on every push; download them from the run's **Artifacts** (`TIMEFLOW-apks`).

---

## Install on a OnePlus tablet

**Via file transfer:** copy `app-debug.apk` (or `app-release.apk`) to the tablet → open it in **Files** → allow *Install unknown apps* for Files when prompted → **Install**.

**Via ADB:** enable *Settings → About device → tap Build number 7×* → *Developer options → USB debugging*, connect USB, then:

```bash
adb install -r app-debug.apk
```

Then open **TIMEFLOW** from the app drawer.

---

## Enable TIMEFLOW as the system screensaver

Menu names vary by OxygenOS version:

1. **Settings → Display & brightness → Screen saver** (older builds: *Settings → Display → Screen saver*; or search Settings for "Screen saver"). The **Screen saver settings** button in TIMEFLOW tries to open this page directly.
2. Turn **Screen saver** on and select **TIMEFLOW Clock**. The gear icon next to it opens TIMEFLOW's settings.
3. **When to start:** *While charging* (best for a tablet on a stand), *While docked*, or *Either*.
4. Tap **Start now** to preview. Touch the screen to exit.

### OxygenOS restrictions and the fallback

- **Some OxygenOS/ColorOS builds hide the Screen saver page** even though Android's screensaver system is still present. TIMEFLOW's service is registered normally, so you can enable it once over ADB:

  ```bash
  adb shell settings put secure screensaver_enabled 1
  adb shell settings put secure screensaver_components com.timeflow.clock/com.timeflow.clock.dream.TimeflowDreamService
  adb shell settings put secure screensaver_activate_on_sleep 1   # start while charging
  adb shell settings put secure screensaver_activate_on_dock 1    # start while docked
  ```

  Check with `adb shell settings get secure screensaver_components`. It should print the TIMEFLOW component.
- **The screensaver only starts when the screen would otherwise time out while charging/docked.** That is Android's rule; no app can trigger the system screensaver on demand without system privileges.
- **If a build has removed the screensaver system entirely**, use **Start clock**: the manual full-screen clock keeps the screen awake with the same design, burn-in protection and night mode. Plug in the tablet and leave it running.
- OxygenOS battery optimisation doesn't affect the screensaver (Android binds it itself), and the manual clock holds the screen on only while it is visible.

---

## Usage tips

- **Exit:** tap anywhere. With **Touch protection** on, single taps are ignored (a hint appears), and you exit by double-tap or long-press.
- **Burn-in:** keep *Pure black* background, a shift interval of 2–5 min, and moderate brightness for OLED.
- **Night mode** dims the clock (and, optionally, the backlight for the clock window only); your normal brightness returns when the clock closes.

---

## Technical notes

- `TimeflowDreamService` implements `LifecycleOwner` and `SavedStateRegistryOwner` so Jetpack Compose runs correctly inside a service window. The dream lifecycle drives the Compose lifecycle, so the clock stops ticking (zero CPU) when dreaming stops.
- The ticker wakes once per second only when seconds or blinking colon are shown, otherwise once per minute on the minute boundary.
- Typography is sized from the actual available width and height (portrait or landscape) and converted with the current density, so the clock never clips or stretches, even with large system font sizes.
- Tabular digits (`tnum`) stop the time from shifting sideways as digits change.
