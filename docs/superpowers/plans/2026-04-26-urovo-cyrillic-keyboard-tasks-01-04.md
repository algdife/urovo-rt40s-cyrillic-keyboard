# Tasks 1-4: Scaffold, KeyMap, Mode, ModeStore

---

## Task 1: Project scaffold + Gradle setup

**Files:**
- Create: `C:/AutomateIt/Urovo/settings.gradle.kts`
- Create: `C:/AutomateIt/Urovo/build.gradle.kts`
- Create: `C:/AutomateIt/Urovo/gradle.properties`
- Create: `C:/AutomateIt/Urovo/.gitignore`
- Create: `C:/AutomateIt/Urovo/app/build.gradle.kts`
- Create: `C:/AutomateIt/Urovo/app/proguard-rules.pro`

- [ ] **Step 1: Initialize git (one time)**

```bash
cd C:/AutomateIt/Urovo
git init
git config user.email "you@example.com"   # only if not already set globally
git config user.name "Your Name"
```

- [ ] **Step 2: Create `.gitignore`**

```
# Android / Gradle
*.iml
.gradle/
local.properties
build/
.idea/
*.apk
*.aab
captures/

# Signing
keystore/*.keystore
keystore/keystore.properties
```

- [ ] **Step 3: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "cyr-toggle"
include(":app")
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

- [ ] **Step 5: Create `gradle.properties`**

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "ru.urovo.cyrtoggle"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.urovo.cyrtoggle"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
}
```

- [ ] **Step 7: Create empty `app/proguard-rules.pro`**

```
# Keep accessibility service entry point
-keep class ru.urovo.cyrtoggle.CyrToggleAccessibilityService { *; }
```

- [ ] **Step 8: Generate Gradle wrapper**

Run:
```bash
gradle wrapper --gradle-version 8.5
```
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.{jar,properties}`.

If `gradle` is not on PATH, install it (or download a wrapper from another Gradle project of yours and copy `gradle/wrapper/` + `gradlew*` over).

- [ ] **Step 9: Verify scaffold builds**

Run:
```bash
./gradlew tasks
```
Expected: prints task list, no errors. (No source files yet — won't try to compile anything.)

- [ ] **Step 10: Commit**

```bash
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties \
        app/build.gradle.kts app/proguard-rules.pro \
        gradle/wrapper/ gradlew gradlew.bat
git commit -m "chore: project scaffold and Gradle setup"
```

---

## Task 2: KeyMap — pure-data lookup with full unit test coverage (TDD)

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/KeyMap.kt`
- Test:   `app/src/test/kotlin/ru/urovo/cyrtoggle/KeyMapTest.kt`

- [ ] **Step 1: Write the failing test (full mapping table from spec §7)**

Create `app/src/test/kotlin/ru/urovo/cyrtoggle/KeyMapTest.kt`:

```kotlin
package ru.urovo.cyrtoggle

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyMapTest {

    private fun base(kc: Int) = KeyMap.lookup(kc, 0)
    private fun shift(kc: Int) = KeyMap.lookup(kc, KeyEvent.META_SHIFT_ON)
    private fun ctrl(kc: Int) = KeyMap.lookup(kc, KeyEvent.META_CTRL_ON)
    private fun ctrlShift(kc: Int) = KeyMap.lookup(kc,
        KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON)

    private fun ch(c: Char) = KeyMap.Lookup(KeyMap.Result.Char, c)
    private val silent = KeyMap.Lookup(KeyMap.Result.Silent, null)
    private val passThrough = KeyMap.Lookup(KeyMap.Result.PassThrough, null)

    @Test fun base_letters() {
        assertEquals(ch('а'), base(KeyEvent.KEYCODE_A))
        assertEquals(ch('б'), base(KeyEvent.KEYCODE_B))
        assertEquals(ch('ц'), base(KeyEvent.KEYCODE_C))
        assertEquals(ch('д'), base(KeyEvent.KEYCODE_D))
        assertEquals(ch('е'), base(KeyEvent.KEYCODE_E))
        assertEquals(ch('ф'), base(KeyEvent.KEYCODE_F))
        assertEquals(ch('г'), base(KeyEvent.KEYCODE_G))
        assertEquals(ch('х'), base(KeyEvent.KEYCODE_H))
        assertEquals(ch('и'), base(KeyEvent.KEYCODE_I))
        assertEquals(ch('й'), base(KeyEvent.KEYCODE_J))
        assertEquals(ch('к'), base(KeyEvent.KEYCODE_K))
        assertEquals(ch('л'), base(KeyEvent.KEYCODE_L))
        assertEquals(ch('м'), base(KeyEvent.KEYCODE_M))
        assertEquals(ch('н'), base(KeyEvent.KEYCODE_N))
        assertEquals(ch('о'), base(KeyEvent.KEYCODE_O))
        assertEquals(ch('п'), base(KeyEvent.KEYCODE_P))
        assertEquals(silent, base(KeyEvent.KEYCODE_Q))
        assertEquals(ch('р'), base(KeyEvent.KEYCODE_R))
        assertEquals(ch('с'), base(KeyEvent.KEYCODE_S))
        assertEquals(ch('т'), base(KeyEvent.KEYCODE_T))
        assertEquals(ch('ю'), base(KeyEvent.KEYCODE_U))
        assertEquals(ch('в'), base(KeyEvent.KEYCODE_V))
        assertEquals(silent, base(KeyEvent.KEYCODE_W))
        assertEquals(silent, base(KeyEvent.KEYCODE_X))
        assertEquals(ch('у'), base(KeyEvent.KEYCODE_Y))
        assertEquals(ch('з'), base(KeyEvent.KEYCODE_Z))
        assertEquals(ch('ъ'), base(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('ь'), base(KeyEvent.KEYCODE_PERIOD))
    }

    @Test fun shift_capitals() {
        assertEquals(ch('А'), shift(KeyEvent.KEYCODE_A))
        assertEquals(ch('Б'), shift(KeyEvent.KEYCODE_B))
        assertEquals(ch('Ц'), shift(KeyEvent.KEYCODE_C))
        assertEquals(ch('Е'), shift(KeyEvent.KEYCODE_E))
        assertEquals(ch('Ю'), shift(KeyEvent.KEYCODE_U))
        assertEquals(ch('У'), shift(KeyEvent.KEYCODE_Y))
        assertEquals(ch('З'), shift(KeyEvent.KEYCODE_Z))
        assertEquals(silent, shift(KeyEvent.KEYCODE_Q))
        assertEquals(silent, shift(KeyEvent.KEYCODE_W))
        assertEquals(silent, shift(KeyEvent.KEYCODE_X))
        assertEquals(ch('ъ'), shift(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('ь'), shift(KeyEvent.KEYCODE_PERIOD))
    }

    @Test fun ctrl_extras() {
        assertEquals(ch('э'), ctrl(KeyEvent.KEYCODE_A))
        assertEquals(ch('ч'), ctrl(KeyEvent.KEYCODE_C))
        assertEquals(ch('ё'), ctrl(KeyEvent.KEYCODE_E))
        assertEquals(ch('щ'), ctrl(KeyEvent.KEYCODE_S))
        assertEquals(ch('ш'), ctrl(KeyEvent.KEYCODE_T))
        assertEquals(ch('ы'), ctrl(KeyEvent.KEYCODE_U))
        assertEquals(ch('я'), ctrl(KeyEvent.KEYCODE_Y))
        assertEquals(ch('ж'), ctrl(KeyEvent.KEYCODE_Z))
        assertEquals(ch(','), ctrl(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('.'), ctrl(KeyEvent.KEYCODE_PERIOD))
        // Letters with no Ctrl extra → pass through (Ctrl+B, Ctrl+D, etc.)
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_B))
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_D))
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_F))
    }

    @Test fun ctrl_shift_capital_extras() {
        assertEquals(ch('Э'), ctrlShift(KeyEvent.KEYCODE_A))
        assertEquals(ch('Ч'), ctrlShift(KeyEvent.KEYCODE_C))
        assertEquals(ch('Ё'), ctrlShift(KeyEvent.KEYCODE_E))
        assertEquals(ch('Щ'), ctrlShift(KeyEvent.KEYCODE_S))
        assertEquals(ch('Ш'), ctrlShift(KeyEvent.KEYCODE_T))
        assertEquals(ch('Ы'), ctrlShift(KeyEvent.KEYCODE_U))
        assertEquals(ch('Я'), ctrlShift(KeyEvent.KEYCODE_Y))
        assertEquals(ch('Ж'), ctrlShift(KeyEvent.KEYCODE_Z))
    }

    @Test fun unmappable_keys_pass_through() {
        assertEquals(passThrough, base(KeyEvent.KEYCODE_0))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_9))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_DEL))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_ENTER))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_TAB))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_F1))
    }
}
```

- [ ] **Step 2: Run tests, verify they FAIL**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests ru.urovo.cyrtoggle.KeyMapTest
```
Expected: compile error (KeyMap doesn't exist yet) or test failures.

- [ ] **Step 3: Implement KeyMap**

Create `app/src/main/kotlin/ru/urovo/cyrtoggle/KeyMap.kt`:

```kotlin
package ru.urovo.cyrtoggle

import android.view.KeyEvent

object KeyMap {

    enum class Result { Char, Silent, PassThrough }

    data class Lookup(val result: Result, val char: Char?) {
        companion object {
            fun ch(c: Char) = Lookup(Result.Char, c)
            val silent = Lookup(Result.Silent, null)
            val passThrough = Lookup(Result.PassThrough, null)
        }
    }

    /**
     * Lookup result for the (keyCode, metaState) pair when in RU mode.
     * Caller has already verified mode == RU and event action == ACTION_DOWN.
     * metaState is masked to (META_SHIFT_ON | META_CTRL_ON).
     */
    fun lookup(keyCode: Int, metaState: Int): Lookup {
        val shift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val ctrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> letter('а', 'А', extra = 'э', extraCap = 'Э', ctrl, shift)
            KeyEvent.KEYCODE_B -> letter('б', 'Б', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_C -> letter('ц', 'Ц', extra = 'ч', extraCap = 'Ч', ctrl, shift)
            KeyEvent.KEYCODE_D -> letter('д', 'Д', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_E -> letter('е', 'Е', extra = 'ё', extraCap = 'Ё', ctrl, shift)
            KeyEvent.KEYCODE_F -> letter('ф', 'Ф', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_G -> letter('г', 'Г', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_H -> letter('х', 'Х', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_I -> letter('и', 'И', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_J -> letter('й', 'Й', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_K -> letter('к', 'К', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_L -> letter('л', 'Л', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_M -> letter('м', 'М', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_N -> letter('н', 'Н', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_O -> letter('о', 'О', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_P -> letter('п', 'П', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_Q -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_R -> letter('р', 'Р', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_S -> letter('с', 'С', extra = 'щ', extraCap = 'Щ', ctrl, shift)
            KeyEvent.KEYCODE_T -> letter('т', 'Т', extra = 'ш', extraCap = 'Ш', ctrl, shift)
            KeyEvent.KEYCODE_U -> letter('ю', 'Ю', extra = 'ы', extraCap = 'Ы', ctrl, shift)
            KeyEvent.KEYCODE_V -> letter('в', 'В', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_W -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_X -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_Y -> letter('у', 'У', extra = 'я', extraCap = 'Я', ctrl, shift)
            KeyEvent.KEYCODE_Z -> letter('з', 'З', extra = 'ж', extraCap = 'Ж', ctrl, shift)
            KeyEvent.KEYCODE_COMMA  -> if (ctrl) Lookup.ch(',') else Lookup.ch('ъ')
            KeyEvent.KEYCODE_PERIOD -> if (ctrl) Lookup.ch('.') else Lookup.ch('ь')
            else -> Lookup.passThrough
        }
    }

    private fun letter(
        lower: Char, upper: Char,
        extra: Char?, extraCap: Char?,
        ctrl: Boolean, shift: Boolean
    ): Lookup = when {
        ctrl && shift && extraCap != null -> Lookup.ch(extraCap)
        ctrl && extra != null              -> Lookup.ch(extra)
        ctrl                                -> Lookup.passThrough
        shift                               -> Lookup.ch(upper)
        else                                -> Lookup.ch(lower)
    }
}
```

- [ ] **Step 4: Run tests, verify they PASS**

Run:
```bash
./gradlew :app:testDebugUnitTest --tests ru.urovo.cyrtoggle.KeyMapTest
```
Expected: BUILD SUCCESSFUL, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/KeyMap.kt \
        app/src/test/kotlin/ru/urovo/cyrtoggle/KeyMapTest.kt
git commit -m "feat: KeyMap with exhaustive RU phonetic mapping and tests"
```

---

## Task 3: Mode enum (top-level)

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/Mode.kt`

- [ ] **Step 1: Create Mode.kt**

```kotlin
package ru.urovo.cyrtoggle

enum class Mode { EN, RU }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/Mode.kt
git commit -m "feat: Mode enum"
```

---

## Task 4: ModeStore — SharedPreferences wrapper (TDD with Robolectric)

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/ModeStore.kt`
- Test:   `app/src/test/kotlin/ru/urovo/cyrtoggle/ModeStoreTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package ru.urovo.cyrtoggle

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModeStoreTest {

    private lateinit var store: ModeStore

    @Before fun setUp() {
        store = ModeStore(ApplicationProvider.getApplicationContext())
        store.set(Mode.EN)
    }

    @Test fun defaults_to_EN() {
        // After clear-and-set-EN, value is EN
        assertEquals(Mode.EN, store.get())
    }

    @Test fun set_persists() {
        store.set(Mode.RU)
        assertEquals(Mode.RU, store.get())
        // New instance reads same SharedPreferences
        val store2 = ModeStore(ApplicationProvider.getApplicationContext())
        assertEquals(Mode.RU, store2.get())
    }

    @Test fun toggle_flips_and_returns_new_mode() {
        assertEquals(Mode.RU, store.toggle())
        assertEquals(Mode.RU, store.get())
        assertEquals(Mode.EN, store.toggle())
        assertEquals(Mode.EN, store.get())
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests ru.urovo.cyrtoggle.ModeStoreTest
```
Expected: compile error (ModeStore not defined).

- [ ] **Step 3: Implement ModeStore**

```kotlin
package ru.urovo.cyrtoggle

import android.content.Context
import android.content.SharedPreferences

class ModeStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(): Mode = Mode.valueOf(prefs.getString(KEY_MODE, Mode.EN.name)!!)

    fun set(mode: Mode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    /** Flips the stored mode and returns the new value. */
    fun toggle(): Mode {
        val newMode = if (get() == Mode.EN) Mode.RU else Mode.EN
        set(newMode)
        return newMode
    }

    companion object {
        private const val PREFS = "cyr_toggle_prefs"
        private const val KEY_MODE = "mode"
    }
}
```

- [ ] **Step 4: Run, verify PASS**

```bash
./gradlew :app:testDebugUnitTest --tests ru.urovo.cyrtoggle.ModeStoreTest
```
Expected: 3 tests passed.

- [ ] **Step 5: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: all KeyMap + ModeStore tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/ModeStore.kt \
        app/src/test/kotlin/ru/urovo/cyrtoggle/ModeStoreTest.kt
git commit -m "feat: ModeStore with SharedPreferences persistence"
```
