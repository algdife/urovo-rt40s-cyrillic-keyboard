# Cyrillic Toggle for Urovo RT40S

Android Accessibility Service that adds Russian phonetic Cyrillic input to the
hardware keypad of Urovo RT40S devices. Toggle EN ↔ RU via **Shift + Space**.

See full design: `docs/superpowers/specs/2026-04-26-urovo-cyrillic-keyboard-design.md`

## Build

Requirements: JDK 17, Android SDK platform 35, Android build-tools 35.

Setup once: copy `env.sh.example` to `env.sh` and edit paths to match your machine, then `source ./env.sh`.

```bash
./gradlew :app:testDebugUnitTest        # run unit tests
./gradlew :app:assembleRelease          # produces signed release APK
```

Output: `app/build/outputs/apk/release/app-release.apk` (and a copy at the project root as `cyr-toggle-1.0.0-release.apk`).

To produce a signed release, create `keystore/cyr-toggle.keystore` and `keystore/keystore.properties` first (see "Signing" below).

## Signing

The build looks for `keystore/keystore.properties` containing:

```
storeFile=keystore/cyr-toggle.keystore
storePassword=...
keyAlias=cyr-toggle
keyPassword=...
```

Generate the keystore once with:

```bash
keytool -genkeypair -alias cyr-toggle -keyalg RSA -keysize 2048 -validity 10000 \
  -keystore keystore/cyr-toggle.keystore -dname "CN=Cyr Toggle, O=Urovo Warehouse, C=RU"
```

Both keystore files are git-ignored — store them somewhere safe and shared with the team responsible for releases.

## Deploy to a single device

```bash
adb install -r cyr-toggle-1.0.0-release.apk
```

Then on device: Settings → Accessibility → Cyrillic Toggle → enable.

## Deploy to a fleet

Push the APK via your MDM. After install, MDM can auto-grant `BIND_ACCESSIBILITY_SERVICE` if it has device-owner privileges.

For SOTI, Scalefusion, etc., create a profile that:
- silent-installs the APK
- enables `ru.urovo.cyrtoggle/.CyrToggleAccessibilityService` in the Secure setting `enabled_accessibility_services`

## Worker instructions

Hand `INSTALL_RU.md` (Russian) to warehouse staff or print it.

## Architecture

Single APK with one AccessibilityService and three supporting classes:
- `KeyMap` — pure-data Russian phonetic lookup table (33 letters across base/Shift/Ctrl/Ctrl+Shift)
- `ModeStore` — SharedPreferences-backed EN/RU mode persistence
- `TextInjector` — 2-tier insertion (`ACTION_SET_TEXT` → clipboard `ACTION_PASTE` fallback) for universal app coverage
- `CyrToggleAccessibilityService` — main runtime, intercepts hardware keys via `canRequestFilterKeyEvents`

The soft (on-screen) keyboard is **not** affected — this APK is not an IME.

## Limitations

- A handful of apps with custom canvas/text rendering may not accept text injection. If you find one, run `adb logcat -s CyrToggle.Inject` while the user types — both tiers will be logged with their result. If both fail consistently in an app, that app is not addressable without a custom IME.
- Mode resets to EN on every boot (intentional — known starting state).
- APK signed with v2 scheme only (sufficient for minSdk 28). If your MDM requires v1, edit `app/build.gradle.kts` to enable JAR signing.

## License

Internal corporate tool. Not for redistribution.
