# Urovo RT40S — Device Knowledge Base

Reference for any future project on this device (e.g., the planned `1c_scan_app`).
Compiled during the Cyrillic Toggle build, 2026-04-26.

## Device identity

- **Model:** Urovo RT40S
- **SKU on hand:** RT40S-13QSE584G51HEU
- **Serial:** `01742517089316`
- **Form factor:** rugged Android handheld with full alphanumeric keypad + touchscreen + barcode scanner
- **Screen:** 4 inch, 480×800 px (per public spec sheet)
- **CPU:** Qualcomm octa-core 2.45 GHz (per public spec sheet)
- **RAM/storage:** 4 GB / 64 GB (per public spec sheet)

## Operating system

- **Android version:** 13 (confirmed via `targetSdk` compatibility tests; vendor confirms via `RT40S` model line)
- **API level:** 33 (compileSdk 35 was used for our build but the device is 33)
- **System language:** Russian (UI labels seen in `dumpsys accessibility`: «Панель навигации», «Настройки USB»)
- **GMS:** present (Google services available — Chrome, Play Store, etc. show up as registered accessibility clients)
- **Pre-installed apps observed in accessibility-clients list:**
  - `com.google.android.marvin.talkback` (TalkBack)
  - `com.android.launcher3`
  - `com.android.chrome`
  - `com.android.mms`
  - `com.google.android.googlequicksearchbox`
  - `com.google.android.apps.wellbeing`
  - `com.google.android.keep`
  - `com.android.settings.intelligence`
  - `com.google.android.apps.photos`
  - `com.google.android.inputmethod.latin` (Gboard)
  - `com.google.android.youtube`
  - `org.codeaurora.snapcam` (CodeAurora camera — vendor-specific)

## Hardware keypad — physical layout

51-key alphanumeric variant. Letters arranged **alphabetically** (A-Z grid), NOT QWERTY. Photo:
`https://kahar.co.id/wp-content/uploads/2025/03/Urovo-RT40S-3.jpg`

### Key sections (top → bottom of keypad)

1. **Top row:** ESC | (orange scan trigger) | TAB
2. **P1 / arrows / P2** row
3. **Numeric keypad** (3x4 grid): 7 8 9 ⌫ ; 4 5 6 SHIFT ; 1 2 3 ENT ; ⌫ 0 . ↵
4. **Letter rows** (alphabetical, ~5 per row): A-E, F-J, K-O, P-T, U-W, X-Y-Z plus CTRL on left, blue square on right (likely SPACE)
5. **Power button** bottom-right

Each letter key has a small blue **F11-F25** label below it — Fn+letter triggers function-key codes.

### Modifiers confirmed present

- **CTRL** — visible top-left of letter section
- **SHIFT** — right side of numeric keypad
- **Fn** — implicit (used for the F11+ codes)
- **Alt** — likely present but not confirmed visually

### Key sections that send Android KeyEvents

To be confirmed empirically per device (next section).

## Software keyboard / IME

The device ships with **Google's Latin Gboard (`com.google.android.inputmethod.latin`)** as the soft keyboard — supports Cyrillic out of the box if the user adds the Russian language.

Known behaviour:
- Soft keyboard works fine for Cyrillic input.
- The `Shift + Space` shortcut documented to switch IME languages on stock Android **does not actually fire** on RT40S — likely because the language switcher is handled at IME level and the Urovo soft keyboard / Gboard is configured with only one language by default.

## Hardware key dispatch — known concerns

This is the area we are actively debugging. Two patterns are common on rugged
Android scanners (Urovo, Honeywell, Zebra) and need to be understood before
building any project that intercepts keys:

1. **Standard Android KeyEvent path** — the kernel input driver fires events
   on `/dev/input/eventX`, the Android InputDispatcher routes them to the
   focused window or to any Accessibility Service that registered with
   `canRequestFilterKeyEvents="true"`.

2. **Vendor broadcast path** — the keypad driver intercepts events and
   instead of dispatching them as KeyEvents, broadcasts them as `Intent`
   actions that any registered `BroadcastReceiver` can listen to. This
   pattern is used to implement programmable keys (e.g., the orange scan
   trigger). The InputDispatcher never sees these events, so accessibility
   key filters never fire on them.

**Open question (as of writing):** which path do the alphabetic keys use?
The barcode scan trigger uses path 2 for sure (Urovo's `ScanManager` SDK
delivers it). Whether A-Z go through path 1 or path 2 is being verified
right now via real-hardware logcat capture.

## Vendor SDK — `KeyMapManager`

Urovo publishes an Android-side SDK that includes:

- **`android.device.ScanManager`** — barcode scanner control. Open/close
  scanner, start/stop decode, configure output mode (intent broadcast vs.
  text-field injection), trigger control, symbology list. Documented at
  `https://en.urovo.com/developer/android/device/ScanManager.html`.

- **`android.device.KeyMapManager`** — remap hardware key/button events at
  system level. Mapped keys can launch applications, send broadcasts, or
  emit different keycodes. Mapping is persistent across reboot. Available
  since Android 4.3 / 5.1. Documented at
  `https://en.urovo.com/developer/android/device/KeyMapManager.html`.
- **`android.device.KeyMapManager.KeyEntry`** — per-key data structure with
  `keyCode` (Android `KeyEvent.getKeyCode()`), `scanCode`
  (`KeyEvent.getScanCode()`), and an `actions` field that can hold an
  `Intent` for launch/broadcast.

Vendor SDK samples on GitHub:
`https://github.com/urovosamples/SDK_ReleaseforAndroid`

The SDK is the canonical way to:
- read raw scanned barcodes into your app
- intercept arbitrary hardware buttons (including the scan trigger and
  programmable keys)
- swap key behaviour at the system level (no accessibility service needed)

For projects that need scanner control or hardware-key reassignment, the
`KeyMapManager` route is **simpler and more reliable** than an
AccessibilityService.

## ADB access

Successfully verified via USB:

- `adb devices` shows: `01742517089316  device  product:RT40S  model:RT40S  device:RT40S`
- USB debugging is enabled on this physical device (one-time setup)
- File-transfer USB mode is required (charging-only mode disables adb)
- The device is authorized for this PC (RSA fingerprint accepted on first
  connect)

Useful adb commands gathered during this session:

```bash
# List installed packages
adb shell pm list packages | grep <something>

# Process list
adb shell ps -A | grep <something>

# Accessibility services state
adb shell dumpsys accessibility | grep -A 1 "Enabled services\|Bound services"

# Enable an accessibility service via adb (bypasses Settings UI)
adb shell settings put secure enabled_accessibility_services <pkg>/<full.class.Name>
adb shell settings put secure accessibility_enabled 1

# Restricted-settings unlock for sideloaded apps that need accessibility:
adb shell appops set <package> ACCESS_RESTRICTED_SETTINGS allow

# Inject a synthetic key event (NOTE: bypasses accessibility key filter)
adb shell input keyevent 29              # KEY_A
adb shell input keycombination 59 62     # SHIFT_LEFT + SPACE

# Stream raw input events (kernel level, before any framework filtering)
adb shell getevent -l

# Dump input device list and recent events
adb shell dumpsys input

# Tail logcat filtered to specific tags
adb logcat -s MyTag.Foo MyTag.Bar
```

## Known gotchas for any future project

1. **Restricted Settings (Android 13+ sideload gate)** — sideloaded APKs
   cannot enable accessibility / notification listener / device admin /
   overlay until the user explicitly taps "Allow restricted settings" in
   the app's three-dot menu in App Info. The menu item only appears if the
   APK was not installed via Play Store. This trips every first-time
   install and has to be in any user-facing install instructions.

2. **`adb shell am force-stop <pkg>`** disables the package's
   accessibility service in the system's enabled-services list. Re-enable
   manually via Settings UI or `adb shell settings put secure
   enabled_accessibility_services ...`.

3. **`adb shell input keyevent` does not pass through accessibility key
   filters.** Synthetic adb input is injected at a lower layer; if you
   need to test an AccessibilityService key filter, you must press real
   hardware keys.

4. **Boot mode auto-reset** — apps that persist mode in
   SharedPreferences should expect first-boot defaults. RT40S boots
   normally; no Direct Boot quirks observed beyond standard Android 13.

5. **Robolectric ceiling.** Robolectric 4.13 supports up to SDK 34; if
   compileSdk on this device's targetSdk is 35, tests must be annotated
   `@Config(sdk = [34])` or they fail to set up the Android shadow.

## Build environment paths (this PC)

- **Android SDK:** `C:/Android/Sdk` (platforms/android-35,
  build-tools/35.0.0, platform-tools, cmdline-tools, NDK)
- **JDK:** `C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot` (JDK 17)
- **Gradle:** 9.4.1 system-wide (via Chocolatey at
  `C:/ProgramData/chocolatey/bin/gradle.exe`); used only to bootstrap the
  per-project wrapper. Per-project wrapper version is whatever the project
  pins (we used 8.10.2).
- **adb:** `C:/Android/Sdk/platform-tools/adb.exe` — works fine over USB
- **apksigner:** `C:/Android/Sdk/build-tools/35.0.0/apksigner.bat` (note: it
  is a `.bat` file, not a Unix executable; from bash, invoke via
  `cmd //c "C:\Android\Sdk\build-tools\35.0.0\apksigner.bat ..."`)

A reusable env script lives at `env.sh.example` in the project root —
copy to `env.sh` (gitignored) and `source ./env.sh` before any gradle/adb
command.

## Sources

- Urovo developer portal — `https://en.urovo.com/developer/`
- Urovo `ScanManager` API — `https://en.urovo.com/developer/android/device/ScanManager.html`
- Urovo `KeyMapManager` API — `https://en.urovo.com/developer/android/device/KeyMapManager.html`
- Urovo SDK samples on GitHub — `https://github.com/urovosamples/SDK_ReleaseforAndroid`
- RT40S product page — `https://en.urovo.com/products/mobile/RT40S.html`
- RT40 user manual (PDF) — `https://manuals.plus/urovo/rt40-industrial-mobile-computer-manual`
- RT40 spec sheet — `https://urovo.eu/wp-content/uploads/2021/07/RT40EN-Spec-20210331.pdf`
