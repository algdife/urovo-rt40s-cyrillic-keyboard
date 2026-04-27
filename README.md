# Cyrillic Toggle for Urovo RT40S

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Russian phonetic Cyrillic input on the **hardware keypad** of the Urovo RT40S, plus a basic on-screen keyboard. Works around Urovo's vendor-level key-event intercept that defeats both standard accessibility and KCM approaches on this device.

## Краткое описание

Печать русских букв с физической клавиатуры терминала Urovo RT40S — фонетическая раскладка (A → а, B → б, …). Готовый APK: [`dist/cyr-toggle-2.0.1-release.apk`](dist/). Инструкция: [`dist/INSTALL_RU.md`](dist/INSTALL_RU.md). Памятка для печати: [`dist/WAREHOUSE_LABEL_RU.docx`](dist/WAREHOUSE_LABEL_RU.docx).

## Cheat sheet

```
A → а   B → б   C → ц   D → д   E → е   F → ф   G → г   H → х
I → и   J → й   K → к   L → л   M → м   N → н   O → о   P → п
R → р   S → с   T → т   U → ю   V → в   Y → у   Z → з
, → ъ   . → ь
Shift+letter        → capital (А, Б, …)
Ctrl+letter         → ё ж ч щ ш ы я э
Ctrl Ctrl + letter  → Ё Ж Ч Щ Ш Ы Я Э (double-tap Ctrl)
Hold Space ½ sec    → toggle EN ⇄ RU
Double-tap Space    → show / hide on-screen keyboard
```

## Install on a device

```bash
adb install -r dist/cyr-toggle-2.0.1-release.apk
adb shell appops set ru.urovo.cyrtoggle ACCESS_RESTRICTED_SETTINGS allow
adb shell ime enable ru.urovo.cyrtoggle/.CyrToggleInputMethodService
adb shell ime set    ru.urovo.cyrtoggle/.CyrToggleInputMethodService
adb shell pm grant   ru.urovo.cyrtoggle android.permission.POST_NOTIFICATIONS
```

Step-by-step recovery instructions (factory reset / new device): [`dist/REINSTALL_FACTORY_RESET.md`](dist/REINSTALL_FACTORY_RESET.md). MDM bulk-deploy notes: same file.

## Build

```bash
cp env.sh.example env.sh    # then edit paths to match your machine
source ./env.sh
./gradlew :app:testDebugUnitTest    # 8 unit tests
./gradlew :app:assembleRelease      # signed APK
```

Requires JDK 17 + Android SDK platform 35. To produce a signed release create `keystore/cyr-toggle.keystore` and `keystore/keystore.properties` (gitignored — see `dist/REINSTALL_FACTORY_RESET.md`).

## Why a custom IME

The standard Android paths (`AccessibilityService` with `canRequestFilterKeyEvents`, custom `KeyCharacterMap` layouts) are silently disabled on the RT40S — the device's `KeyMapManager.dispatchUdroidKeyEvent` intercepts every keypad event upstream of both. A transparent `InputMethodService` is the only non-root mechanism that reliably fires `onKeyDown` for hardware-keypad keys. Full reverse-engineering write-up in [`FINAL_REPORT.md`](FINAL_REPORT.md). Device knowledge base for any future Urovo project in [`docs/URovo-RT40S-scanner-info.md`](docs/URovo-RT40S-scanner-info.md).

## License

[MIT](LICENSE). Pull requests welcome — especially for other Urovo / UBX / Mindeo / iData device variants.
