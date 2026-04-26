# Reinstall after factory reset (or on a fresh device)

This walks an IT person through restoring Cyrillic Toggle on a Urovo RT40S
that has been factory-reset, or setting it up on a brand-new RT40S.

Estimated time: 5 minutes per device.

## You'll need

- The RT40S device.
- A USB cable.
- A Windows / Mac / Linux PC with `adb` (Android platform-tools) installed.
- The file `cyr-toggle-1.0.0-release.apk` (this folder).

## Step 1 — Enable USB debugging on the RT40S

1. Settings → About phone → tap **Build number** 7 times until "You are now
   a developer" appears.
2. Settings → System → Developer options → enable **USB debugging**.
3. Plug the device into the PC via USB. On the device, accept the
   "Allow USB debugging from this computer?" dialog. Tick "Always allow".
4. On the PC: `adb devices` should list the RT40S.

## Step 2 — Install the APK

```bash
adb install -r cyr-toggle-1.0.0-release.apk
```

Expected output: `Success`.

## Step 3 — Unlock restricted settings

On Android 13, sideloaded apps that need accessibility / IME-binding
permissions are blocked by default. Unlock via adb (faster than the GUI):

```bash
adb shell appops set ru.urovo.cyrtoggle ACCESS_RESTRICTED_SETTINGS allow
```

## Step 4 — Enable and activate the IME

```bash
adb shell ime enable ru.urovo.cyrtoggle/.CyrToggleInputMethodService
adb shell ime set    ru.urovo.cyrtoggle/.CyrToggleInputMethodService
```

(Both lines together set Cyrillic Toggle as the active input method.)

## Step 5 — Grant notification permission (optional but recommended)

```bash
adb shell pm grant ru.urovo.cyrtoggle android.permission.POST_NOTIFICATIONS
```

This makes the "EN / РУ" mode indicator visible in the status bar.

## Step 6 — Verify

```bash
adb shell settings get secure default_input_method
```

Expected output:
```
ru.urovo.cyrtoggle/.CyrToggleInputMethodService
```

Now have a worker open any text field and:
- Press hardware **A** → expect **a** (still in EN mode by default).
- Hold hardware **Space** for ~½ second → expect a "РУ" toast.
- Press **A** → expect **а**.

## Optional — disable Developer options after setup

If your IT policy requires Developer options off:
- Settings → System → Developer options → toggle **OFF**.
- USB debugging will be disabled. This does NOT affect the IME — it remains
  installed and active.

## Bulk deployment via MDM

If you have many devices and an MDM with device-owner enrollment
(SOTI / Scalefusion / Hexnode / etc.), the same steps can be automated:

1. Push `cyr-toggle-1.0.0-release.apk` via MDM "silent install".
2. MDM applies these Secure settings:
   - `enabled_input_methods` includes `ru.urovo.cyrtoggle/.CyrToggleInputMethodService`
   - `default_input_method` = `ru.urovo.cyrtoggle/.CyrToggleInputMethodService`
3. MDM grants `appops ACCESS_RESTRICTED_SETTINGS allow` for the package.

The exact UI varies per MDM but the underlying settings keys are the same.

## File checksums (to verify download integrity)

Run this from the folder containing the APK:

```bash
sha256sum cyr-toggle-1.0.0-release.apk
```

If you ever distribute the APK over an untrusted channel, compare the
checksum here against the one printed by the IT-side build.

## If something goes wrong

- `adb` not found → install Android platform-tools.
- `adb devices` empty → bad cable, USB debugging not enabled, or device
  not authorized. Re-do step 1.
- `Failure` on install → the device already has a different version with a
  different signing key. Uninstall first: `adb uninstall ru.urovo.cyrtoggle`.
- After install, IME doesn't appear in Settings → re-do step 3 (restricted
  settings unlock).
- IME enabled but typing still produces nothing → verify
  `default_input_method` matches step 6. If not, re-run the `ime set`
  command from step 4.

## Building from source (only if the APK is lost)

The full source code lives at the URL printed in the project README.
Clone, then:

```bash
./gradlew :app:assembleRelease
```

Requires JDK 17, Android SDK platform 35, and `keystore/keystore.properties`
with the team-shared signing key.
