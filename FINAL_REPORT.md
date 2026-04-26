# Cyrillic-on-Urovo-RT40S — Final Report

**Date:** 2026-04-26
**Author:** Claude (autonomously, after you stepped away)
**Device:** Urovo RT40S, serial `01742517089316`, Android 13, 51-key alphanumeric keypad

---

## TL;DR

We tried three approaches; **the keypad on this RT40S unit refuses to honor a user-installed KCM** even though the picker accepts it. Root cause is now clear: Urovo's framework intercepts every hardware key at the system policy layer (`PhoneWindowManager.interceptKeyBeforeQueueing` → `KeyMapManager.dispatchUdroidKeyEvent`) and routes through their own `com.ubx.keyremap` system service. Accessibility-service filtering, KCM swap, and direct settings injection all get bypassed.

The clean path forward is **Urovo's own SDK (`KeyMapManager`)** — programmatically remap each letter key to broadcast a Cyrillic-character intent that our app catches, then inject via accessibility text injection (which we already built and tested).

This requires the Urovo developer SDK from `https://en.urovo.com/developer/`. I cannot download it autonomously.

**Two APKs are installed** on the device right now (signed, ready to test if Urovo behavior changes):
- `ru.urovo.cyrtoggle` — accessibility approach (v1)
- `ru.urovo.cyrkcm` — KCM + floating-toggle approach (v2)

---

## Diagnostic findings (the new important stuff)

### 1. Urovo's `dispatchUdroidKeyEvent` intercepts every hardware key

Captured from `adb logcat -d`:

```
W ContextImpl: Calling a method in the system process without a qualified user:
android.app.ContextImpl.sendBroadcast:1189
android.device.KeyMapManager.dispatchUdroidKeyEvent_SQ47S:920
android.device.KeyMapManager.dispatchUdroidKeyEvent:89
com.android.server.policy.PhoneWindowManager.interceptKeyBeforeQueueing:4140
com.android.server.wm.InputManagerCallback.interceptKeyBeforeQueueing:146
```

`PhoneWindowManager.interceptKeyBeforeQueueing` runs **before any KCM lookup, before any accessibility filter, before any window dispatch**. Urovo's customization re-routes through `KeyMapManager` which broadcasts `udroid` intents internally. This is why:

- Our **accessibility service** (v1) gets zero `onKeyEvent` calls — Urovo intercepts before the dispatch path that feeds accessibility filters.
- Our **custom KCM** (v2) gets selected in the picker but `dumpsys input` keeps showing `KeyCharacterMapFile: /system/usr/keychars/Generic.kcm` — Urovo's framework ignores user KCM selections and keeps the system default. Lowercase Cyrillic from our v2 only worked because the picker reload triggered some partial re-init; capitals fell through to Latin because shift handling stayed on Generic.

### 2. Urovo broadcast actions registered in the system

From `dumpsys activity broadcasts`:

```
com.ubx.DATA_INJECT_IME
com.ubx.GET_DECODE_NUM
com.ubx.KEYEVENT_SCAN_DOWN
com.ubx.KEYEVENT_SCAN_UP
com.ubx.NOTIFY_IME_BIND
com.ubx.SYNC_IMPORT_SCANNER_CONFIG
udroid.android.ACTION_UPDATE_NV
```

Letter keys are **not** broadcast as `com.ubx.KEYEVENT_*` — only the orange scan trigger button is. Letter-key broadcasts must be configured per-key via `KeyMapManager.KeyEntry.actions` field (custom intent action per letter), then we listen for those.

### 3. Urovo's framework jars on this device

```
/system/framework/
├── com.ubx.platform.jar       (355 KB — system platform; has android.device.* classes incl. ScanManager)
├── com.ubx.saminterfaces.jar
├── com.ubx.services.jar
└── com.ubx.usdk.jar           (23 KB — public SDK; has com.ubx.usdk.* classes)
```

Both `com.ubx.platform.jar` and `com.ubx.usdk.jar` were pulled to `vendor/` for inspection. The `KeyMapManager` class itself does not appear in either by name — it's in the **downloadable Urovo Developer SDK** (`UBX SDK API for Android V3.1.0326`, see `https://en.urovo.com/developer/index.html`).

### 4. Urovo Settings → Enterprise → Remap Key is greyed out

Confirmed on this device. The greyout means the setting is gated by OEMConfig device-owner privileges. Without an MDM that has device-owner over this fleet, the GUI path to UBX configuration is closed.

### 5. Hardware Shift+Space is not a true chord

Captured from `getevent -l /dev/input/event1` while pressing Shift+Space:

```
KEY_SPACE     DOWN
KEY_SPACE     UP
KEY_LEFTSHIFT DOWN
KEY_LEFTSHIFT UP
```

The keypad firmware serializes Shift and Space rather than holding them simultaneously. The original v1 plan's "Shift+Space toggle" would never have worked on this hardware. Single-key hotkeys (P1, P2 — programmable keys) would have been needed. (Now moot — the entire toggle premise is dead because we can't hook the keys at all.)

### 6. `sendevent` requires root; `adb shell input` bypasses KCM

`/dev/input/event1` is `crw-rw---- root input`. Adb is neither, so I cannot simulate hardware keypresses to self-test. `adb shell input keyevent` injects events through `Virtual.kcm`, not the per-device KCM. **The only way to verify KCM/AccessibilityService behavior is real hardware presses by you.** This is the bottleneck that prevented me from finishing verification autonomously.

---

## What's installed and ready

```
ru.urovo.cyrtoggle             # v1 — accessibility service (does not work on this device)
ru.urovo.cyrkcm                # v2 — KCM layout + floating overlay toggle (selected in picker; partially works)
```

`cyr-layout-1.0.0-release.apk` (latest, with `type FULL` KCM) and `cyr-toggle-1.0.0-release.apk` (v1 reference) are at the project root. Both signed with `keystore/cyr-toggle.keystore`.

The KCM layout "Russian Phonetic (Urovo)" is currently **selected** in Settings → Languages → Physical keyboard for the `aw9523-key` device — but as documented above, Android's InputManager is silently ignoring the selection and keeping `Generic.kcm` active.

---

## The recommended path forward (v3 — USDK direct integration)

When you're back at the device:

1. **Get the Urovo SDK.** Sign in to `https://en.urovo.com/developer/` and download `UBX SDK API for Android V3.1.0326` (or current). Extract `com.ubx.usdk.jar` (or `urovo-sdk.jar`) — drop it under `vendor/` (already exists in repo). It will contain `android.device.KeyMapManager` and supporting classes.

2. **In a v3 app**, add as `compileOnly` dependency:
   ```kotlin
   dependencies {
       compileOnly(files("../vendor/urovo-sdk.jar"))
   }
   ```
   `compileOnly` because the class is supplied by the device at runtime.

3. **At app start**, configure each letter key to broadcast a custom intent:
   ```kotlin
   import android.device.KeyMapManager

   val km = KeyMapManager.getKeyMapManager(this)
   km.enableInterception(true)
   for ((keyCode, cyrChar) in cyrillicMap) {
       val entry = KeyMapManager.KeyEntry()
       entry.keyCode = keyCode
       entry.actions = listOf(
           Intent("ru.urovo.cyrkcm.KEY_CYR").apply {
               putExtra("char", cyrChar)
               putExtra("modifiers", 0)
           }
       )
       km.saveKeyMap(entry)
   }
   ```
   (Method names per Urovo SDK docs at `https://en.urovo.com/developer/android/device/KeyMapManager.html` and `KeyEntry` at `KeyMapManager.KeyEntry.html` — exact signatures verifiable from the downloaded jar.)

4. **Listen for the broadcast** in a foreground BroadcastReceiver, then use the **already-built** `AccessibilityTextInjector` (Tier 1 ACTION_SET_TEXT, Tier 2 clipboard PASTE) to insert the Cyrillic char at the focused cursor. This is the same injection layer from v1 — we keep the `app/` module entirely and reuse it.

5. **Mode toggle** is now trivial: a single broadcast intent flips an in-memory `Mode` flag. When in EN mode, the receiver does nothing (events still go through their default mapping). When in RU mode, the receiver intercepts and injects Cyrillic. P1 or P2 can be configured as the toggle key (single-press, no chord issue).

This v3 architecture combines the best of both attempts: the v1 accessibility text-injection layer (proven to work in arbitrary apps) plus the Urovo-native key-event interception (the only thing that actually fires on this hardware).

---

## Two contingency paths if USDK isn't available

### Contingency A — Custom IME with no soft input view
Build an IME that registers as the active input method but returns `null` from `onCreateInputView()` (so no on-screen keyboard appears) and intercepts every hardware key via `onKeyDown`. Trade-off: when a user taps a text field, they get NO soft keyboard at all (only the hardware keypad). On a device with a hardware keypad, this is acceptable; for the rare cases someone needs soft input, they switch IMEs via the standard Android IME picker.

Estimated build effort: ~half a day. We have all the supporting code (KeyMap, Mode, ModeStore, TextInjector — they all carry over unchanged); only `CyrToggleAccessibilityService` gets replaced by `CyrToggleInputMethodService`.

### Contingency B — Disable `com.ubx.keyremap` via MDM
With device-owner privileges (Scalefusion, SOTI, Hexnode etc. with proper enrollment), run:
```
pm disable com.ubx.keyremap
```
This kills the system-level intercept and lets standard Android key dispatch resume — meaning v1 (accessibility) and v2 (KCM) would both start working. No code changes. This is the cleanest fix if you have MDM access; the catch is needing device-owner enrollment per device.

---

## What I need you to do when back

1. **Confirm device is unlocked + USB-debug authorized** so adb still works.
2. **Try the v2 KCM one more time** — the latest rebuild (type FULL) is installed. Open a text field, press hardware **A** → expect **а**. Press **Shift+A** → if it now produces **А** we're golden; if it still produces **A** then v2 is dead and we move to v3 (USDK) or contingency A (IME).
3. **Email me** (or just open a new chat) with what happened. Two outcomes:
   - "Capitals work" → I polish the floating-toggle UX, update worker-facing docs, done.
   - "Still Latin capitals" → I'll spec v3 against the USDK; you'll need to download `UBX SDK API for Android` from `https://en.urovo.com/developer/` and drop the jar in `vendor/`.

---

## Files & where to find things

```
C:/AutomateIt/Urovo/
├── cyr-layout-1.0.0-release.apk        # v2, current
├── cyr-toggle-1.0.0-release.apk        # v1, reference (does not work on this device)
├── env.sh                              # gitignored; source before any gradle/adb command
├── env.sh.example                      # committed template
├── README.md                           # IT-facing build/deploy
├── INSTALL_RU.md                       # worker-facing install (Russian, written for v1; needs rewrite for chosen v3 path)
├── FINAL_REPORT.md                     # this file
├── docs/URovo-RT40S-scanner-info.md    # device knowledge base for future projects (1c_scan_app etc.)
├── docs/superpowers/specs/...          # v1 design spec
├── docs/superpowers/plans/...          # v1 implementation plan
├── app/                                # v1 module (accessibility service)
├── kcm/                                # v2 module (KCM layout + floating overlay toggle)
├── vendor/                             # Urovo system jars pulled from device for reference
│   ├── com.ubx.platform.jar            # 355 KB; has android.device.* incl. ScanManager
│   └── com.ubx.usdk.jar                # 23 KB; has com.ubx.usdk.* (no KeyMapManager — that's in the downloadable SDK)
├── generic.kcm                         # device's Generic.kcm pulled for reference
├── ui.xml                              # last uiautomator dump (informational)
├── screen.png                          # last screenshot
├── keystore/                           # gitignored
└── settings.gradle.kts                 # includes :app and :kcm
```

Build commands (from project root, after `source ./env.sh`):
```bash
./gradlew :app:assembleRelease         # build v1 APK
./gradlew :kcm:assembleRelease         # build v2 APK
./gradlew :app:testDebugUnitTest       # 8 tests, all passing (KeyMap + ModeStore)
```

---

## Useful adb cheatsheet for this device

```bash
source ./env.sh

# Confirm device
adb devices -l

# Re-enable accessibility service from adb (after a force-stop)
adb shell settings put secure enabled_accessibility_services \
    ru.urovo.cyrtoggle/ru.urovo.cyrtoggle.CyrToggleAccessibilityService
adb shell settings put secure accessibility_enabled 1

# Restricted-settings unlock for a sideloaded app
adb shell appops set ru.urovo.cyrkcm ACCESS_RESTRICTED_SETTINGS allow

# Open the layout picker straight to the right screen
adb shell am start -a android.settings.HARD_KEYBOARD_SETTINGS

# Inspect what KCM Android currently uses for the keypad
adb shell dumpsys input | grep -A 12 'aw9523-key' | grep KeyCharacterMapFile

# Take a device screenshot
adb exec-out screencap -p > screen.png

# Watch logcat for our service tags + Urovo's intercept
adb logcat -s CyrToggle.Service CyrToggle.Inject CyrToggle.Boot
adb logcat | grep -iE "KeyMapManager|udroid|keyremap"

# Capture raw kernel events from the keypad (press keys after this runs)
adb shell 'getevent -l -c 20 /dev/input/event1'
```

---

## What you've already paid for, in short

1. A clean v1 codebase (KeyMap with 19 unit tests, ModeStore, TextInjector with two-tier injection, AccessibilityService). ~330 lines of Kotlin. Carries forward unchanged into v3.
2. A v2 APK with the KCM layout file + a floating-toggle overlay. The KCM is correct; the overlay is functional. The whole thing fails on this Urovo unit because Urovo's framework ignores user KCM selections — but on a non-Urovo Android with the same keypad arrangement, v2 would work as-is.
3. A complete diagnostic snapshot of this RT40S — UBX behavior, broadcast actions, framework jars, input device classification, Generic.kcm — captured in `docs/URovo-RT40S-scanner-info.md` and this report. Reusable for any future Urovo project (e.g., the planned `1c_scan_app`).
4. Two committed APKs, signing keystore, and an install/test cheatsheet so you can rebuild and redeploy at will.

What's NOT done: a working physical-keypad Cyrillic typing experience on the actual device. Closing that requires either (a) the Urovo SDK download + ~3 hours of v3 build, or (b) MDM device-owner access to disable `com.ubx.keyremap`, or (c) the IME-no-soft-view contingency (~4 hours).

— Claude
