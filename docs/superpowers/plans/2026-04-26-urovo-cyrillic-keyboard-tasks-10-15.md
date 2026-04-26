# Tasks 10-15: Manifest, SettingsActivity, Build, Install/Test, Worker Instructions

---

## Task 10: AndroidManifest

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/layout/activity_settings.xml`

- [ ] **Step 1: Create themes.xml**

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.CyrToggle" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:statusBarColor">?attr/colorSurface</item>
    </style>
</resources>
```

- [ ] **Step 2: Create activity_settings.xml** (placeholder; SettingsActivity uses it in Task 11)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:id="@+id/statusText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:gravity="center"
        android:paddingBottom="32dp" />

    <Button
        android:id="@+id/openSettingsButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/open_settings" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:paddingTop="32dp"
        android:textSize="12sp"
        android:text="v1.0.0" />
</LinearLayout>
```

- [ ] **Step 3: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.CyrToggle">

        <activity
            android:name=".SettingsActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".CyrToggleAccessibilityService"
            android:exported="true"
            android:label="@string/accessibility_service_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <receiver
            android:name=".BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/res/layout/activity_settings.xml
git commit -m "feat: AndroidManifest — service, receiver, launcher activity"
```

---

## Task 11: SettingsActivity

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/SettingsActivity.kt`

- [ ] **Step 1: Implement**

```kotlin
package ru.urovo.cyrtoggle

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.openSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = if (isAccessibilityEnabled()) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val running = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return running.any { it.id?.contains(packageName) == true }
    }
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/SettingsActivity.kt
git commit -m "feat: SettingsActivity shows enabled status, opens system settings"
```

---

## Task 12: Build a debug APK end-to-end (sanity check)

- [ ] **Step 1: Assemble debug APK**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit (no source changes; this is a checkpoint)**

If everything is green, no commit needed — proceed to Task 13.

If something is broken, fix it now before proceeding to release signing.

---

## Task 13: Generate signing keystore + build signed release APK

**Files:**
- Create: `keystore/cyr-toggle.keystore` (gitignored)
- Create: `keystore/keystore.properties` (gitignored)

- [ ] **Step 1: Create keystore directory**

```bash
mkdir -p keystore
```

- [ ] **Step 2: Generate keystore**

Run (you will be prompted for passwords; choose strong ones and store them in your password manager):

```bash
keytool -genkeypair \
  -alias cyr-toggle \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -keystore keystore/cyr-toggle.keystore \
  -dname "CN=Cyr Toggle, O=Urovo Warehouse, C=RU"
```
Expected: keystore file created.

- [ ] **Step 3: Create keystore.properties**

`keystore/keystore.properties`:

```properties
storeFile=keystore/cyr-toggle.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=cyr-toggle
keyPassword=YOUR_KEY_PASSWORD
```

Replace placeholders with the passwords you used in step 2. This file is **gitignored** — do not commit it.

- [ ] **Step 4: Build signed release APK**

```bash
./gradlew :app:assembleRelease
```
Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 5: Rename APK for distribution**

```bash
cp app/build/outputs/apk/release/app-release.apk cyr-toggle-1.0.0-release.apk
```

- [ ] **Step 6: Verify signature**

```bash
jarsigner -verify -verbose -certs cyr-toggle-1.0.0-release.apk | head -20
```
Expected: "jar verified."

- [ ] **Step 7: Commit (only the rename target metadata)**

The keystore + properties are gitignored. Just confirm `.gitignore` excludes them and there's nothing to commit. The APK itself is also gitignored (treat as build artifact; distribute through MDM/file share).

---

## Task 14: Manual on-device test

This task is the **definitive verification** that the universal-app coverage requirement is met. Cannot be automated meaningfully.

- [ ] **Step 1: Install APK on RT40S**

Connect device via USB. Enable USB debugging on the device (Settings → About → tap Build number 7 times → back to Settings → Developer options → USB debugging).

```bash
adb devices                                    # confirm device shows
adb install -r cyr-toggle-1.0.0-release.apk
```
Expected: "Success".

- [ ] **Step 2: Enable accessibility service**

On the device:
1. Open the **Cyrillic Toggle** app from launcher.
2. Tap **Open Accessibility Settings**.
3. Find **Cyrillic Toggle** in the list, toggle ON, accept the warning dialog.
4. Return to the Cyrillic Toggle app — status should now show "is enabled".

- [ ] **Step 3: Smoke test — alphabet round trip**

Open any text field (e.g., a note app, Chrome address bar, or the device Settings search box). Press **Shift+Space** — toast shows "RU".

Type the entire alphabet on the hardware keypad in order: a b c d e f g h i j k l m n o p q r s t u v w x y z

Expected output in the field: `абцдефгхийклмноп рстющ узз`

Wait — actually q/w/x produce nothing. Expected literal output: `абцдефгхийклмнопрстющвуз` (where the `q`, `w`, `x` keys produce nothing — gaps appear nowhere because no character is inserted).

Press Shift+Space — toast shows "EN". Type the same sequence — expect Latin `abcdefghijklmnopqrstuvwxyz`.

- [ ] **Step 4: Smoke test — capitals**

In RU mode, hold Shift and type the alphabet → expect `АБЦДЕФГХИЙКЛМНОП РСТЮВ УЗ` (capitals; q/w/x silent).

- [ ] **Step 5: Smoke test — Ctrl extras**

In RU mode:
- Ctrl+a → `э`
- Ctrl+c → `ч`
- Ctrl+e → `ё`
- Ctrl+s → `щ`
- Ctrl+t → `ш`
- Ctrl+u → `ы`
- Ctrl+y → `я`
- Ctrl+z → `ж`
- Ctrl+, → `,` (literal comma)
- Ctrl+. → `.` (literal period)
- Ctrl+Shift+a → `Э`, Ctrl+Shift+z → `Ж`, etc.

- [ ] **Step 6: Smoke test — punctuation**

In RU mode: comma key → `ъ`, period key → `ь`. Then with Ctrl: comma → `,`, period → `.`.

- [ ] **Step 7: Smoke test — pass-through**

In **both** modes:
- Digits 0–9 type literal digits.
- Backspace deletes a character.
- Enter submits / newlines.
- Arrow keys move cursor.
- Scan trigger reads a barcode normally.
- Ctrl+C copies, Ctrl+V pastes (in apps that support these — only on letters with no Cyrillic extra: B, D, F, G, H, etc.).
- ESC, TAB, P1, P2, F-keys behave as before.

- [ ] **Step 8: Universal-app coverage check**

Repeat steps 3–7 in each of these contexts. For each, confirm the alphabet round trip works:

| App | Tier expected | Pass? |
|-----|---------------|-------|
| Settings → search box | Tier 1 | |
| Chrome → address bar | Tier 1 | |
| Chrome → page with `<input>` (e.g., google.com search) | Tier 1 or 2 | |
| Messages / SMS app | Tier 1 | |
| Files app → rename a file | Tier 1 | |
| 1C mobile / inventory app (whatever the warehouse uses) | Tier 1 or 2 | |

If any app fails, capture `logcat` and triage:

```bash
adb logcat -s CyrToggle.Service CyrToggle.Inject
```
Logs will say which tier was attempted and whether it succeeded. If both tiers fail in a particular app, that app is using a custom view that doesn't expose accessibility actions — note it as a known limitation in `README.md`.

- [ ] **Step 9: Boot reset test**

Set mode to RU (Shift+Space, confirm toast). Reboot the device:

```bash
adb reboot
```

Wait for boot. Open a text field, type a letter — expect Latin (mode reset to EN). Press Shift+Space → toast "RU" → mode now RU again.

- [ ] **Step 10: If everything passes, tag the release**

```bash
git tag -a v1.0.0 -m "First release: Cyrillic Toggle for Urovo RT40S"
```

If something failed, fix it and rebuild from Task 13 step 4.

---

## Task 15: Worker-facing install/use instructions (Russian + English)

**Files:**
- Create: `INSTALL_RU.md` (warehouse worker / supervisor — Russian)
- Create: `README.md` (IT staff / build & deploy — English)

- [ ] **Step 1: Create INSTALL_RU.md**

```markdown
# Кириллица на Urovo RT40S — установка и использование

## Что это

Маленькая программа, которая позволяет печатать русский текст с физической
клавиатуры терминала. Кнопка **A** даёт **а**, **B** даёт **б**, и так далее.

Переключение между латиницей и кириллицей — **Shift + Space**.
На экране появится короткая надпись **EN** или **RU**, чтобы было понятно, в каком режиме клавиатура.

## Установка (один раз для каждого терминала)

1. Подключите терминал к компьютеру по USB **или** скопируйте файл
   `cyr-toggle-1.0.0-release.apk` на терминал любым удобным способом
   (флешка через OTG, файлообменник, MDM).
2. На терминале откройте проводник, найдите файл `cyr-toggle-1.0.0-release.apk`,
   нажмите на него.
3. Если появится предупреждение «Установка из неизвестных источников» —
   разрешите.
4. После установки откройте приложение **Cyrillic Toggle** в меню приложений.
5. Нажмите кнопку **Open Accessibility Settings**.
6. Найдите в списке **Cyrillic Toggle** и **включите** его. Согласитесь с
   предупреждением системы (это требование Android — программа сможет читать
   текст на экране, иначе она не сможет вставлять буквы).
7. Готово. Закройте настройки.

## Как пользоваться (каждый день)

- Когда нужна **латиница** — просто печатайте как обычно.
- Когда нужен **русский** — нажмите **Shift + Space**. Появится надпись **RU**.
  Теперь A → а, B → б, и т.д.
- Чтобы вернуться к **латинице** — снова нажмите **Shift + Space**. Появится **EN**.

### Заглавные буквы

- **Shift + буква** = заглавная (например, Shift+A → А).

### «Лишние» русские буквы (которых нет в латинице)

Удерживайте **Ctrl** и нажмите букву:

| Комбинация | Буква |
|------------|-------|
| Ctrl + A | э |
| Ctrl + C | ч |
| Ctrl + E | ё |
| Ctrl + S | щ |
| Ctrl + T | ш |
| Ctrl + U | ы |
| Ctrl + Y | я |
| Ctrl + Z | ж |
| Ctrl + Shift + A | Э (заглавная) |
| (то же для остальных) | заглавная вариант |

### Твёрдый и мягкий знаки

- Клавиша **запятая** (,) в режиме РУ = **ъ** (твёрдый знак)
- Клавиша **точка** (.) в режиме РУ = **ь** (мягкий знак)
- Если нужны настоящие запятая или точка — **Ctrl + ,** или **Ctrl + .**

### Кнопки Q, W, X

В режиме РУ ничего не делают (для них нет соответствующих русских букв в нашей раскладке). В режиме EN работают как обычная латиница.

## Что НЕ изменилось

- Сканер штрихкодов работает как раньше.
- Цифры, стрелки, Enter, Backspace — всё как раньше.
- Экранная клавиатура (когда вы нажимаете на поле ввода — выскакивает клавиатура внизу) — без изменений.

## Если что-то не работает

1. **Не переключается на русский** — откройте **Настройки → Специальные возможности → Cyrillic Toggle** и убедитесь, что переключатель включён.
2. **После перезагрузки сразу латиница** — это нормально, всегда после загрузки терминал в режиме EN. Нажмите Shift + Space.
3. **Буквы вообще не печатаются** — обновите приложение и перезагрузите терминал. Если не помогло, обратитесь в IT.
```

- [ ] **Step 2: Create README.md (IT-facing)**

```markdown
# Cyrillic Toggle for Urovo RT40S

Android Accessibility Service that adds Russian phonetic Cyrillic input to the
hardware keypad of Urovo RT40S devices. Toggle EN ↔ RU via **Shift + Space**.

See full design: `docs/superpowers/specs/2026-04-26-urovo-cyrillic-keyboard-design.md`

## Build

Requirements: JDK 17, Android SDK platform 34, Android build-tools 34.

```bash
./gradlew :app:testDebugUnitTest        # run unit tests
./gradlew :app:assembleRelease          # produces signed release APK
```

Output: `app/build/outputs/apk/release/app-release.apk`

To produce a signed release, create `keystore/cyr-toggle.keystore` and
`keystore/keystore.properties` first (see "Signing" below).

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

Both keystore files are git-ignored — store them somewhere safe and shared
with the team responsible for releases.

## Deploy to a single device

```bash
adb install -r cyr-toggle-1.0.0-release.apk
```

Then on device: Settings → Accessibility → Cyrillic Toggle → enable.

## Deploy to a fleet

Push the APK via your MDM. After install, MDM can auto-grant
`BIND_ACCESSIBILITY_SERVICE` if it has device-owner privileges.

For SOTI, Scalefusion, etc., create a profile that:
- silent-installs the APK
- enables `ru.urovo.cyrtoggle/.CyrToggleAccessibilityService` in the
  Secure setting `enabled_accessibility_services`

## Worker instructions

Hand `INSTALL_RU.md` (Russian) to warehouse staff or print it.

## Limitations

- A handful of apps with custom canvas/text rendering may not accept text
  injection. If you find one, run `adb logcat -s CyrToggle.Inject` while the
  user types — both tiers will be logged with their result. If both fail
  consistently in an app, that app is not addressable without a custom IME.

- Mode resets to EN on every boot (intentional — known starting state).

## License

Internal corporate tool. Not for redistribution.
```

- [ ] **Step 3: Commit**

```bash
git add INSTALL_RU.md README.md
git commit -m "docs: worker-facing install (Russian) + IT-facing README"
```

- [ ] **Step 4: Final state check**

```bash
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleRelease
git status
git log --oneline
```
Expected: tests green, release builds, clean working tree, ~13 commits.

---

## Self-review checklist (run after writing the plan)

**Spec coverage:**
- §4 Architecture → Tasks 1, 8 (service), 11 (SettingsActivity), 9 (BootReceiver) ✓
- §5 Data flow → Task 8 ✓
- §6 Text injection (2 tiers) → Task 5 ✓
- §7 Key mapping → Task 2 (KeyMap + exhaustive tests) ✓
- §8 Edge cases → Task 8 (toggle rate-limit, mode persistence on connect, modifier-only pass-through), Task 14 (universal-app smoke test, boot reset test) ✓
- §9 Testing → Tasks 2, 4 (unit), 14 (manual on device) ✓
- §10 Build & artifacts → Tasks 1, 12, 13 ✓
- §11 Worker install → Task 15 ✓
- §12 Open items deferred → not in plan, correct

**Placeholder scan:** all code blocks contain real code. No "TODO", "TBD", or "implement later". Each step shows actual command or content. ✓

**Type consistency:** `KeyMap.Result`, `KeyMap.Lookup`, `Mode`, `ModeStore`, `TextInjector.insert(Char): Boolean`, `Toaster.show(Mode)`, `CyrToggleAccessibilityService` — names used in later tasks match those defined earlier. ✓
