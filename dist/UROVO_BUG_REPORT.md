# Bug report — RT40S keypad Cyrillic input is broken at the framework level

**To:** Urovo Technical Support — `urovo@urovo.com`
**Web form:** https://en.urovo.com/contact/index.html (category: Technical Support)
**Subject:** RT40S — custom hardware-keypad keyboard layouts and accessibility key filters non-functional (Android 13)

---

## Device

- **Model:** Urovo RT40S
- **SKU:** RT40S-13QSE584G51HEU
- **Serial:** 01742517089316
- **Android version:** 13 (API 33)
- **Keypad variant:** 51-key alphanumeric (alphabetical A–Z grid)
- **Kernel input device:** `aw9523-key`, `/dev/input/event1`, classified `KEYBOARD | ALPHAKEY`, KeyboardType=2 (alphabetic)

## Summary

On the RT40S, the standard Android mechanisms for adapting hardware-keypad
input (custom KCM layouts, AccessibilityService key filters) are silently
disabled by the device's framework customization. This appears to be a
side-effect of Urovo's `KeyMapManager` system intercept, which routes every
hardware key event through `dispatchUdroidKeyEvent` from
`PhoneWindowManager.interceptKeyBeforeQueueing` *before* standard Android
dispatch.

Customers who need to type non-Latin scripts (Russian Cyrillic in our case)
on the hardware keypad currently have no supported, non-root path. We worked
around this by writing a custom transparent IME, but standard Android
mechanisms should also work and currently do not.

## Reproduction — issue 1: custom KCM layouts ignored

1. Sign in to a development workstation; install Android Studio + adb.
2. Build a tiny APK that registers a custom Key Character Map via the
   standard mechanism:
   ```xml
   <receiver android:name="android.hardware.input.InputManager"
             android:exported="true">
       <intent-filter>
           <action android:name="android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS"/>
       </intent-filter>
       <meta-data android:name="android.hardware.input.metadata.KEYBOARD_LAYOUTS"
                  android:resource="@xml/keyboard_layouts"/>
   </receiver>
   ```
   Reference an `@xml/keyboard_layouts` containing a `<keyboard-layout>` that
   points to a `type FULL` `.kcm` resource.
3. Install on RT40S:
   ```bash
   adb install -r mylayout.apk
   ```
4. Open Settings → Languages & input → Physical keyboard → tap the keypad
   row (`aw9523-key`). The custom layout DOES appear in the picker.
5. Select the custom layout. Confirm with `dumpsys input`:
   ```bash
   adb shell dumpsys input | grep -A 12 'aw9523-key' | grep KeyCharacterMapFile
   ```
6. **Expected:** `KeyCharacterMapFile: /data/.../mylayout.kcm` (or the
   resource path of the new layout).
   **Actual:** `KeyCharacterMapFile: /system/usr/keychars/Generic.kcm` —
   the framework silently keeps Generic active despite the user-visible
   selection.

Control test that proves the picker UI is functional: select the AOSP
built-in **"Russian"** (JCUKEN) layout. That selection IS honored — pressing
hardware **A** produces **ф** as expected. So Urovo allows AOSP layouts but
silently drops user-installed third-party layouts.

## Reproduction — issue 2: accessibility key filter never fires

1. Build an `AccessibilityService` with `canRequestFilterKeyEvents="true"`
   and the corresponding `flagRequestFilterKeyEvents` flag in
   `accessibility_service_config.xml`.
2. Install, enable in Settings → Accessibility, accept restricted-settings
   unlock.
3. Verify the service is bound and registered:
   ```bash
   adb shell dumpsys accessibility | grep -A 1 "Bound services"
   ```
   Output confirms our service with `capabilities=9`
   (`CAN_RETRIEVE_WINDOW_CONTENT | CAN_REQUEST_FILTER_KEY_EVENTS`).
   Also confirms `Enabled features of Display [0] = [KeyboardInterceptor]`.
4. Press any hardware key (letter, Space, etc.).
5. **Expected:** `AccessibilityService.onKeyEvent` is called.
   **Actual:** `onKeyEvent` is **never called** for keypad events. Verified
   by adding `Log.i` at top of `onKeyEvent` and capturing `adb logcat`.

## Root cause (visible in logcat)

```
W ContextImpl: Calling a method in the system process without a qualified user:
android.app.ContextImpl.sendBroadcast:1189
android.device.KeyMapManager.dispatchUdroidKeyEvent_SQ47S:920
android.device.KeyMapManager.dispatchUdroidKeyEvent:89
com.android.server.policy.PhoneWindowManager.interceptKeyBeforeQueueing:4140
com.android.server.wm.InputManagerCallback.interceptKeyBeforeQueueing:146
```

Urovo's `KeyMapManager.dispatchUdroidKeyEvent` is hooked into
`PhoneWindowManager.interceptKeyBeforeQueueing`. This intercept fires for
every keypad event upstream of:
- the `IInputFilter` chain (where accessibility key filters hook), and
- Android's KCM lookup for the per-device layout.

The intercept appears to broadcast the event internally (via the system
service `com.ubx.keyremap`, UID `system`) rather than consume it, but the
side-effect is that user-supplied KCMs and accessibility filters never see
the event. The standard Android key-handling pipeline is effectively dead
on this device for hardware keypad keys.

## Additional hardware-firmware quirk

The keypad firmware does NOT deliver Shift+Space as a true chord. Captured
via raw `getevent -l /dev/input/event1`:

```
KEY_SPACE     DOWN
KEY_SPACE     UP
KEY_LEFTSHIFT DOWN
KEY_LEFTSHIFT UP
```

The two keys are serialized despite simultaneous physical press. Any
documentation or app that assumes Shift+Space chord (e.g. for IME language
switching) cannot work on this hardware.

## Impact

Customers deploying RT40S to non-English warehouses (Russia, Ukraine,
Belarus, Kazakhstan, Bulgaria, etc.) cannot use standard Android tooling to
add Cyrillic input to the hardware keypad. They must either:
1. Engineer a custom IME (non-trivial; we did this and now have a working
   APK at https://github.com/agazarov-cyber/urovo-rt40s-cyrillic-keyboard),
2. Use Urovo's `KeyMapManager` SDK directly (requires SDK familiarity and
   typically a per-key `KeyEntry` configuration with `Intent` actions), or
3. Obtain device-owner MDM privileges to disable `com.ubx.keyremap`.

None of these are within reach for typical IT departments.

## Requests

1. **Document this behavior** prominently in the RT40S developer guide so
   future integrators don't waste days assuming standard Android mechanisms
   work. Ideally include a worked example of using `KeyMapManager` for
   non-Latin scripts.
2. **Consider providing an OEMConfig (or Settings) toggle** to disable the
   `KeyMapManager` intercept for customers who prefer to use standard
   Android key-handling. Currently Settings → Enterprise featured setting
   → Remap Key is greyed out without device-owner privileges.
3. **Allow user-installed KCM layouts to actually take effect** when the
   user selects them in the Physical-keyboard picker. The current behavior
   (selection accepted in UI, silently ignored at runtime) is misleading.
4. **Publish the full UBX SDK including `KeyMapManager`** on the public
   `urovosamples` GitHub repo. Currently only the older
   `platform_sdk_v4.1.0326.jar` is mirrored; `KeyMapManager.KeyEntry` is
   documented at en.urovo.com/developer but the jar that contains it is
   only on-device under `/system/framework/`.

## Reference / further detail

Full diagnostic + working solution code:
**https://github.com/agazarov-cyber/urovo-rt40s-cyrillic-keyboard**

Specifically:
- `FINAL_REPORT.md` — full reverse-engineering write-up
- `docs/URovo-RT40S-scanner-info.md` — device knowledge base
- `app/src/main/kotlin/ru/urovo/cyrtoggle/CyrToggleInputMethodService.kt` —
  the working IME

Happy to provide additional logcat / dumpsys output, screen recordings, or
discuss directly. Please confirm receipt and let us know whether this is on
the roadmap.

---

Sender: [your name]
Company: [your company]
Country: [your country]
Number of RT40S devices in deployment: [N]
Date: 2026-04-26
