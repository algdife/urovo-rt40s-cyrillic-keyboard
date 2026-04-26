# Tasks 5-9: TextInjector, Toaster, accessibility XML, Service, BootReceiver

---

## Task 5: TextInjector interface + AccessibilityTextInjector (Tier 1 + Tier 2)

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/TextInjector.kt`
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/AccessibilityTextInjector.kt`

This task has no automated tests — `AccessibilityNodeInfo` is hard to mock meaningfully. Verified in Task 13 (manual on-device test).

- [ ] **Step 1: Create TextInjector interface**

```kotlin
package ru.urovo.cyrtoggle

interface TextInjector {
    /** Inserts [char] at the current cursor in the focused editable field.
     *  Returns true on success, false if no field could be reached. */
    fun insert(char: Char): Boolean
}
```

- [ ] **Step 2: Create AccessibilityTextInjector with Tier 1 (ACTION_SET_TEXT)**

```kotlin
package ru.urovo.cyrtoggle

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityTextInjector(private val service: AccessibilityService) : TextInjector {

    private val handler = Handler(Looper.getMainLooper())
    private val clipboard: ClipboardManager =
        service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun insert(char: Char): Boolean {
        val focused = findFocusedEditable() ?: run {
            Log.d(TAG, "no focused editable")
            return false
        }
        if (tryActionSetText(focused, char)) {
            Log.d(TAG, "tier1 ACTION_SET_TEXT ok for '$char'")
            return true
        }
        if (tryClipboardPaste(focused, char)) {
            Log.d(TAG, "tier2 ACTION_PASTE ok for '$char'")
            return true
        }
        Log.w(TAG, "all tiers failed for '$char'")
        return false
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isEditable }
    }

    private fun tryActionSetText(node: AccessibilityNodeInfo, char: Char): Boolean {
        if (!node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }) {
            return false
        }
        val current = node.text?.toString() ?: ""
        val selStart = node.textSelectionStart.coerceAtLeast(0).coerceAtMost(current.length)
        val selEnd = node.textSelectionEnd.coerceAtLeast(selStart).coerceAtMost(current.length)
        val newText = buildString {
            append(current, 0, selStart)
            append(char)
            append(current, selEnd, current.length)
        }
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        // Move cursor to selStart + 1
        val cursorArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selStart + 1)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selStart + 1)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
        return true
    }

    private fun tryClipboardPaste(node: AccessibilityNodeInfo, char: Char): Boolean {
        if (!node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }) {
            return false
        }
        val savedClip = clipboard.primaryClip
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, char.toString()))
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        // Restore the user's clipboard after a short delay so paste completes first.
        handler.postDelayed({
            try {
                if (savedClip != null) clipboard.setPrimaryClip(savedClip)
            } catch (e: Exception) {
                Log.w(TAG, "clipboard restore failed: ${e.message}")
            }
        }, 100)
        return ok
    }

    companion object {
        private const val TAG = "CyrToggle.Inject"
        private const val CLIP_LABEL = "cyr-toggle"
    }
}
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/TextInjector.kt \
        app/src/main/kotlin/ru/urovo/cyrtoggle/AccessibilityTextInjector.kt
git commit -m "feat: TextInjector with ACTION_SET_TEXT primary and clipboard PASTE fallback"
```

---

## Task 6: Toaster

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/Toaster.kt`
- Modify: `app/src/main/res/values/strings.xml` (created here)

- [ ] **Step 1: Create strings.xml**

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Cyrillic Toggle</string>
    <string name="accessibility_service_label">Cyrillic Toggle</string>
    <string name="accessibility_service_description">
        Adds Russian phonetic Cyrillic input to the hardware keypad.
        Press Shift + Space to switch between EN and RU.
    </string>
    <string name="toast_mode_en">EN</string>
    <string name="toast_mode_ru">RU</string>
    <string name="status_enabled">Cyrillic Toggle is enabled.</string>
    <string name="status_disabled">Cyrillic Toggle is NOT enabled. Tap below to grant accessibility permission.</string>
    <string name="open_settings">Open Accessibility Settings</string>
</resources>
```

- [ ] **Step 2: Create Toaster.kt**

```kotlin
package ru.urovo.cyrtoggle

import android.content.Context
import android.widget.Toast

class Toaster(private val context: Context) {

    private var lastToast: Toast? = null

    fun show(mode: Mode) {
        lastToast?.cancel()
        val text = when (mode) {
            Mode.EN -> context.getString(R.string.toast_mode_en)
            Mode.RU -> context.getString(R.string.toast_mode_ru)
        }
        lastToast = Toast.makeText(context, text, Toast.LENGTH_SHORT).also { it.show() }
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/kotlin/ru/urovo/cyrtoggle/Toaster.kt
git commit -m "feat: Toaster shows current mode briefly"
```

---

## Task 7: Accessibility service config XML

**Files:**
- Create: `app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Create the config**

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewFocused|typeViewTextChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagRequestFilterKeyEvents"
    android:canRequestFilterKeyEvents="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="50"
    android:summary="@string/accessibility_service_label" />
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/xml/accessibility_service_config.xml
git commit -m "feat: accessibility service XML config (key filtering enabled)"
```

---

## Task 8: CyrToggleAccessibilityService — the runtime

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/CyrToggleAccessibilityService.kt`

- [ ] **Step 1: Implement the service**

```kotlin
package ru.urovo.cyrtoggle

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class CyrToggleAccessibilityService : AccessibilityService() {

    private lateinit var modeStore: ModeStore
    private lateinit var injector: TextInjector
    private lateinit var toaster: Toaster
    private var lastToggleAtMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        modeStore = ModeStore(this)
        injector = AccessibilityTextInjector(this)
        toaster = Toaster(this)
        Log.i(TAG, "service connected, mode=${modeStore.get()}")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Toggle: Shift + Space, on key DOWN, rate-limited.
        if (event.keyCode == KeyEvent.KEYCODE_SPACE
            && event.action == KeyEvent.ACTION_DOWN
            && (event.metaState and KeyEvent.META_SHIFT_ON) != 0
        ) {
            val now = SystemClock.uptimeMillis()
            if (now - lastToggleAtMs >= TOGGLE_RATE_LIMIT_MS) {
                lastToggleAtMs = now
                val newMode = modeStore.toggle()
                toaster.show(newMode)
                Log.i(TAG, "toggle → $newMode")
            }
            return true   // consume regardless (don't let Shift+Space leak as space)
        }

        // EN mode: full pass-through.
        if (modeStore.get() == Mode.EN) return false

        // Only letters + comma + period are candidates in RU mode.
        if (!isMappableKeyCode(event.keyCode)) return false

        // We act on DOWN; consume UP silently to keep the event stream balanced.
        if (event.action == KeyEvent.ACTION_UP) return true
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val metaMasked = event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON)
        val lookup = KeyMap.lookup(event.keyCode, metaMasked)
        return when (lookup.result) {
            KeyMap.Result.PassThrough -> false
            KeyMap.Result.Silent -> true
            KeyMap.Result.Char -> {
                val char = lookup.char ?: return false
                injector.insert(char)   // success or not, we still consume
                true
            }
        }
    }

    private fun isMappableKeyCode(kc: Int): Boolean = when (kc) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_PERIOD -> true
        else -> false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* unused */ }
    override fun onInterrupt() { /* unused */ }

    companion object {
        private const val TAG = "CyrToggle.Service"
        private const val TOGGLE_RATE_LIMIT_MS = 50L
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/CyrToggleAccessibilityService.kt
git commit -m "feat: CyrToggleAccessibilityService — main runtime"
```

---

## Task 9: BootReceiver

**Files:**
- Create: `app/src/main/kotlin/ru/urovo/cyrtoggle/BootReceiver.kt`

- [ ] **Step 1: Implement**

```kotlin
package ru.urovo.cyrtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            ModeStore(context).set(Mode.EN)
            Log.i("CyrToggle.Boot", "mode reset to EN")
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/ru/urovo/cyrtoggle/BootReceiver.kt
git commit -m "feat: BootReceiver resets mode to EN on boot"
```
