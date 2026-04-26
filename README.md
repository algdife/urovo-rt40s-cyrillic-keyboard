# Cyrillic Toggle for Urovo RT40S

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Russian phonetic Cyrillic input for the **hardware keypad** of Urovo RT40S
rugged Android scanners. Implemented as a transparent Android IME — the only
mechanism that bypasses Urovo's vendor-level key-event intercept.

```
A → а   B → б   C → ц   D → д   E → е   F → ф   G → г   H → х
I → и   J → й   K → к   L → л   M → м   N → н   O → о   P → п
R → р   S → с   T → т   U → ю   V → в   Y → у   Z → з
, → ъ   . → ь
Shift+letter → capital   Ctrl+letter → ё ж ч щ ш ы я э   Ctrl Ctrl + letter → Ё Ж Ч Щ Ш Ы Я Э
Hold Space ½ sec → toggle EN ⇄ RU
```

## Краткое описание (русский)

Маленькое Android-приложение для терминала **Urovo RT40S**, которое позволяет
печатать русский текст с физической клавиатуры. Раскладка фонетическая
(A → а, B → б, …). Переключение между EN и РУ — удерживайте пробел ½ секунды.

Готовый APK для warehouse-сотрудников: [`dist/cyr-toggle-1.0.0-release.apk`](dist/).
Полная инструкция на русском: [`dist/INSTALL_RU.md`](dist/INSTALL_RU.md).
Печатная памятка для рабочих: [`dist/WAREHOUSE_LABEL_RU.docx`](dist/WAREHOUSE_LABEL_RU.docx).

## Why a custom IME (and not an accessibility service or KCM)

The Urovo RT40S runs a customized Android 13 in which the system policy layer
intercepts every hardware-keypad event via
`KeyMapManager.dispatchUdroidKeyEvent`, called from
`PhoneWindowManager.interceptKeyBeforeQueueing`. Consequence:

- **Standard `AccessibilityService` with `canRequestFilterKeyEvents`** never
  receives `onKeyEvent` calls for the keypad — the vendor intercept happens
  upstream of the accessibility filter.
- **Custom `KeyCharacterMap` (.kcm) layouts** appear in
  Settings → Languages → Physical keyboard, can be selected by the user, but
  Android silently keeps `Generic.kcm` active (`dumpsys input` confirms).

Both standard paths are dead on this device. **A transparent IME** —
`InputMethodService` with `onCreateInputView()` returning `null` — is the only
non-root mechanism whose `onKeyDown` reliably fires for hardware keypad keys.
The IME commits Cyrillic via `InputConnection.commitText`. See
`docs/URovo-RT40S-scanner-info.md` and `FINAL_REPORT.md` for the full
diagnostic journey.

## What the APK does

- Registers as an InputMethod (`CyrToggleInputMethodService`) with no soft
  input view — when the user focuses a text field, no on-screen keyboard
  appears; input comes from the hardware keypad.
- In **EN mode** (default): commits the standard ASCII char for the pressed
  key via a hardcoded Latin map (independent of whichever KCM is active).
- In **RU mode**: looks up the Cyrillic char in `KeyMap` and commits it.
- **Long-press Space (≥ 400 ms)** toggles EN ↔ RU. Required because the
  RT40S firmware serializes the Shift+Space chord and there is no Caps Lock
  key on this keypad.
- **Double-tap Ctrl** within 400 ms arms capital-extra mode for the next
  letter (workaround: the firmware drops the Ctrl bit when Shift is also
  held, so the standard Ctrl+Shift+letter combination doesn't deliver to the
  IME). 
- Persistent status-bar notification + Toast on toggle so the worker always
  knows the current mode.
- Mode resets to EN on every boot (intentional, known starting state).

## Build

Requirements: JDK 17, Android SDK platform 35, Android build-tools 35.

```bash
cp env.sh.example env.sh         # then edit paths to match your machine
source ./env.sh
./gradlew :app:testDebugUnitTest # 8 unit tests, all passing
./gradlew :app:assembleRelease   # signed release APK
```

Output: `app/build/outputs/apk/release/app-release.apk`.

To produce a signed release, create `keystore/cyr-toggle.keystore` and
`keystore/keystore.properties` (gitignored). See `README.md` "Signing" below.

## Signing

The build looks for `keystore/keystore.properties`:

```properties
storeFile=keystore/cyr-toggle.keystore
storePassword=...
keyAlias=cyr-toggle
keyPassword=...
```

Generate the keystore once:

```bash
keytool -genkeypair -alias cyr-toggle -keyalg RSA -keysize 2048 -validity 10000 \
  -keystore keystore/cyr-toggle.keystore \
  -dname "CN=Cyr Toggle, O=Urovo Warehouse, C=RU"
```

## Deploy to a single device

```bash
adb install -r dist/cyr-toggle-1.0.0-release.apk
adb shell appops set ru.urovo.cyrtoggle ACCESS_RESTRICTED_SETTINGS allow
adb shell ime enable ru.urovo.cyrtoggle/.CyrToggleInputMethodService
adb shell ime set    ru.urovo.cyrtoggle/.CyrToggleInputMethodService
adb shell pm grant   ru.urovo.cyrtoggle android.permission.POST_NOTIFICATIONS
```

Full step-by-step in [`dist/REINSTALL_FACTORY_RESET.md`](dist/REINSTALL_FACTORY_RESET.md).

## Deploy to a fleet via MDM

Push the APK via SOTI / Scalefusion / Hexnode / etc. with these device-owner
settings applied:

- Silent-install `ru.urovo.cyrtoggle`.
- Add to Secure setting `enabled_input_methods`:
  `ru.urovo.cyrtoggle/.CyrToggleInputMethodService`.
- Set Secure setting `default_input_method` to the same value.
- Grant `appops ACCESS_RESTRICTED_SETTINGS` allow.
- Grant `android.permission.POST_NOTIFICATIONS`.

## Architecture

```
Hardware keypress on aw9523-key
       ↓
Kernel /dev/input/event1
       ↓
InputReader → InputDispatcher
       ↓
PhoneWindowManager.interceptKeyBeforeQueueing
       ↓
Urovo's KeyMapManager.dispatchUdroidKeyEvent  ← OBSERVES & broadcasts;
       ↓                                         does not consume
Window/IME dispatch
       ↓
CyrToggleInputMethodService.onKeyDown        ← we hook here
       │
       ├── Long-press Space (≥ 400 ms) → toggle mode
       ├── Double-tap Ctrl (≤ 400 ms apart) → arm capital-extra
       ├── EN mode: commit Latin via mapEnglish() lookup
       └── RU mode: commit Cyrillic via KeyMap.lookup() → InputConnection.commitText()
```

Source layout:

```
app/src/main/kotlin/ru/urovo/cyrtoggle/
├── CyrToggleInputMethodService.kt   # transparent IME (the runtime)
├── KeyMap.kt                        # pure-data RU phonetic lookup (33 letters × 4 modifier states)
├── Mode.kt, ModeStore.kt            # EN/RU state, persisted to SharedPreferences
├── Toaster.kt                       # toast + persistent notification on mode change
├── BootReceiver.kt                  # resets mode to EN on boot
├── ToggleReceiver.kt                # adb-driven mode flip (diagnostic)
├── SettingsActivity.kt              # launcher icon: enable/switch IME buttons

app/src/main/res/
├── xml/method.xml                   # IME meta-data (ru_RU subtype)
└── values/strings.xml, layout/...

app/src/test/kotlin/ru/urovo/cyrtoggle/
├── KeyMapTest.kt                    # 5 Robolectric tests, exhaustive mapping coverage
└── ModeStoreTest.kt                 # 3 tests for default, persistence, toggle
```

## Tested device

| Device | Urovo RT40S, model `RT40S-13QSE584G51HEU` |
|---|---|
| Android | 13 (API 33) |
| Keypad | 51-key alphanumeric (alphabetical A–Z grid, NOT QWERTY), `aw9523-key` on `/dev/input/event1` |
| Vendor framework | `com.ubx.keyremap`, `com.ubx.platform`, `com.ubx.usdk` |

## Reusable knowledge for other Urovo / Mindeo / iData / UBX projects

See [`docs/URovo-RT40S-scanner-info.md`](docs/URovo-RT40S-scanner-info.md)
for the device's input-pipeline quirks, broadcast actions, framework jar
locations, and adb cheatsheet — useful for any future project that touches
the hardware keypad on this device family.

## Limitations

- The on-screen keyboard is replaced by "no keyboard" while our IME is
  active. Switch IMEs via the standard Android picker if you need a soft
  keyboard for emoji or long Cyrillic text.
- Mode resets to EN on every boot (by design — known starting state).
- APK is v2-signed (sufficient for minSdk 28).

## License

[MIT](LICENSE) — use freely. Pull requests welcome, especially for other
Urovo / UBX / Mindeo / iData device variants.

## Credits

Reverse-engineering work documented in `FINAL_REPORT.md`. Built with the
help of LLM-assisted debugging (Claude, ChatGPT, Gemini cross-checking the
hardware-key intercept findings).
