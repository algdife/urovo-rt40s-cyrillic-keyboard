# Urovo RT40S Cyrillic Hardware Keyboard Toggle — Design

**Date:** 2026-04-26
**Status:** Approved (pending user review of this document)
**Target device:** Urovo RT40S-13QSE584G51HEU (Android 13, 51-key alphanumeric keypad, alphabetical A–Z grid layout)

---

## 1. Problem

The Urovo RT40S has a 51-key alphanumeric hardware keypad with letters arranged alphabetically (A–Z grid, not QWERTY). Out of the box:

- The hardware keypad supports only one language (Latin) at a time.
- Cyrillic cannot be assigned to it through Settings — the only available physical-keyboard layouts come from Urovo and none of them maps the printed alphabetical letters to corresponding Cyrillic letters.
- The documented `Shift+Space` language-switch shortcut does not actually toggle anything on this device.
- Switching language otherwise requires diving into Settings → Languages & input → Physical keyboard, which is not workable for warehouse staff.

The on-screen (soft) keyboard already works correctly for Cyrillic and is **not in scope** for this project. Only the hardware keypad behavior must change.

## 2. Goals

1. Pressing the **A** key produces **а**, **B** produces **б**, etc. (phonetic mapping aligned with the printed letters).
2. **Shift + Space** instantly toggles between Latin (default) and Cyrillic modes.
3. Latin mode is **bit-for-bit identical** to the current behavior — Urovo's existing keypad programming is the source of truth and must not change when the toggle is in EN mode.
4. The on-screen soft keyboard must be **completely unaffected** by this solution.
5. The solution must work in **all apps** the warehouse uses — no per-app whitelisting.
6. Installable as a normal user-installable APK (no root, no platform signing key required).

## 3. Non-goals

- Changing the soft (on-screen) keyboard.
- Supporting languages other than Russian.
- Modifying behavior of digits, arrows, function keys, scan trigger, P1/P2, ESC, TAB, Enter, Backspace.
- Publishing the APK on Google Play (sideload / MDM only).

## 4. Architecture

A single APK `ru.urovo.cyrtoggle` (~200 KB, no third-party dependencies) containing:

- **`CyrToggleAccessibilityService`** — the runtime. Declared with `canRequestFilterKeyEvents="true"` and `canRetrieveWindowContent="true"` in its config XML. Receives every hardware key event before any IME or app does. Returning `true` from `onKeyEvent` consumes the event; returning `false` lets it pass through normally.
- **`KeyMap`** — pure-data lookup `(KeyEvent.keyCode × metaState) → Result`, where `Result ∈ { Cyrillic char, SILENT, PASS_THROUGH }`. Hardcoded in code; no runtime configuration file.
- **`ModeStore`** — wraps `SharedPreferences` for current mode (EN/RU). Read on `onServiceConnected`, written on toggle.
- **`Toaster`** — short `Toast` showing "RU" or "EN" at toggle time.
- **`TextInjector`** — three-tier strategy for inserting a Cyrillic character at the focused cursor position (see §6).
- **`SettingsActivity`** — single-screen launcher icon. Shows whether the accessibility service is enabled, with a button that opens system Settings → Accessibility. No other UI.
- **`BootReceiver`** — `BOOT_COMPLETED` resets mode to EN.

**Permissions in manifest:**
- `BIND_ACCESSIBILITY_SERVICE` (system-protected, granted via Settings UI)
- `RECEIVE_BOOT_COMPLETED`

No internet, no storage outside `SharedPreferences`, no analytics, no PII collection.

## 5. Data flow

```
Hardware key press
        │
        ▼
Android dispatcher
        │
        ▼
CyrToggleAccessibilityService.onKeyEvent(event)
        │
        ├── Is it Shift+Space (DOWN event, with rate-limit ≥50 ms)?
        │       └── Yes → ModeStore.toggle(); Toaster.show("RU"|"EN"); return TRUE
        │
        ├── Is current mode == EN?
        │       └── Yes → return FALSE  (full pass-through, original Latin behavior)
        │
        ├── Is keyCode in {A..Z, COMMA, PERIOD}?
        │       └── No → return FALSE  (digits, arrows, Enter, Backspace, ESC, TAB,
        │                                P1, P2, F-keys, scan trigger all unaffected)
        │
        ├── Is event action == ACTION_UP?
        │       └── Yes → return TRUE  (silently consume; we already injected on DOWN)
        │
        └── ACTION_DOWN of mappable key:
                ├── Resolve Result = KeyMap[keyCode][metaState & (SHIFT|CTRL)]
                ├── If SILENT     →  return TRUE  (consume, no output)
                ├── If PASS_THROUGH →  return FALSE (Ctrl+letter shortcuts like Ctrl+C work)
                └── If Cyrillic char →  TextInjector.insert(char); return TRUE
```

## 6. Text injection (universal app coverage)

This is a hard requirement: must work in *every* app a warehouse worker may type into. Without `INJECT_EVENTS` (system-only) we have two reliable injection paths; we use both, in order, on every translated keystroke:

**Tier 1 — `AccessibilityNodeInfo.ACTION_SET_TEXT`** (primary)
- `findFocus(AccessibilityNodeInfo.FOCUS_INPUT)` on the active window.
- Read current `text` and `textSelectionStart` / `textSelectionEnd`.
- Build new text = `currentText[0..selStart] + cyrChar + currentText[selEnd..]`.
- `performAction(ACTION_SET_TEXT, args{ARGUMENT_SET_TEXT_CHARSEQUENCE = newText})`.
- `performAction(ACTION_SET_SELECTION, args{ARGUMENT_SELECTION_START = selStart+1, ARGUMENT_SELECTION_END = selStart+1})` to advance the cursor.
- Works in standard `EditText`-based apps and in modern WebView `<input>` fields when accessibility is enabled (~95 % of all Android apps including 1C mobile, inventory apps, browsers, system apps).

**Tier 2 — Clipboard + `ACTION_PASTE`** (fallback)
- Save current clipboard contents.
- Place the single Cyrillic char on clipboard via `ClipboardManager`.
- `focusedNode.performAction(ACTION_PASTE)`.
- After 100 ms, restore the original clipboard.
- Catches the remaining cases: custom views that implement `onTextContextMenuItem(android.R.id.paste)` but don't handle `ACTION_SET_TEXT` correctly, some legacy WebViews, certain Compose text fields under specific configurations.

The injector tries Tier 1 first; falls through to Tier 2 only if Tier 1 returns `false` or the focused node has no `ACTION_SET_TEXT` action available. Each tier emits a `logcat` line tagged `CyrToggle.Inject` so we can see in field debugging which tier handled which app.

If both tiers fail (no focused editable field, read-only field, no paste action available): event is dropped silently, no error popup. Worker is expected to tap the field first.

Note: the synthesized-key approach (constructing a `KeyEvent` with Unicode char and dispatching it) is **not** available — `Instrumentation.sendKeySync` and `InputManager.injectInputEvent` both require `INJECT_EVENTS`, a `signature|privileged` permission. Accessibility services cannot inject raw key events into the input stream. The two tiers above are sufficient in practice.

## 7. Key mapping (final)

Cyrillic mode (`mode == RU`). EN mode is full pass-through.

| Android keyCode | base | + Shift (capital) | + Ctrl (extra) | + Ctrl+Shift |
|---|---|---|---|---|
| KEYCODE_A | а | А | э | Э |
| KEYCODE_B | б | Б | pass | pass |
| KEYCODE_C | ц | Ц | ч | Ч |
| KEYCODE_D | д | Д | pass | pass |
| KEYCODE_E | е | Е | ё | Ё |
| KEYCODE_F | ф | Ф | pass | pass |
| KEYCODE_G | г | Г | pass | pass |
| KEYCODE_H | х | Х | pass | pass |
| KEYCODE_I | и | И | pass | pass |
| KEYCODE_J | й | Й | pass | pass |
| KEYCODE_K | к | К | pass | pass |
| KEYCODE_L | л | Л | pass | pass |
| KEYCODE_M | м | М | pass | pass |
| KEYCODE_N | н | Н | pass | pass |
| KEYCODE_O | о | О | pass | pass |
| KEYCODE_P | п | П | pass | pass |
| KEYCODE_Q | SILENT | SILENT | pass | pass |
| KEYCODE_R | р | Р | pass | pass |
| KEYCODE_S | с | С | щ | Щ |
| KEYCODE_T | т | Т | ш | Ш |
| KEYCODE_U | ю | Ю | ы | Ы |
| KEYCODE_V | в | В | pass | pass |
| KEYCODE_W | SILENT | SILENT | pass | pass |
| KEYCODE_X | SILENT | SILENT | pass | pass |
| KEYCODE_Y | у | У | я | Я |
| KEYCODE_Z | з | З | ж | Ж |
| KEYCODE_COMMA | ъ | ъ | , | , |
| KEYCODE_PERIOD | ь | ь | . | . |
| KEYCODE_SPACE | (space) | **toggle EN/RU** | (space) | (space) |

Notes:
- `pass` = return `false` (Android handles natively; preserves Ctrl+C copy / Ctrl+V paste / Ctrl+A select-all on letters that have no Cyrillic extra).
- `SILENT` on q/w/x = consumed, no character output. (No vibration unless requested later.)
- All other key codes (digits, arrows, ESC, TAB, P1, P2, F1–F25, Backspace, Enter, scan trigger): always pass through in both modes.

## 8. Edge cases

- **No focused editable field** — drop event silently.
- **Read-only field** — `ACTION_SET_TEXT` returns false; tiers 2 & 3 also fail; drop silently.
- **Cursor in middle of text** — splice character at cursor and restore selection one position right (Tier 1).
- **Long-press / key repeat** — translate every repeat event (matches Latin behavior).
- **Modifier key alone** (Shift down, Ctrl down) — pass through so other apps see meta state.
- **Shift+Space rapid double-press** — rate-limit toggle to one per 50 ms.
- **Service killed under memory pressure** — `ModeStore` reads from `SharedPreferences` on `onServiceConnected`; mode is restored automatically.
- **Boot** — `BootReceiver` resets to EN.
- **Screen sleep / wake** — no change. Mode persists.
- **APK update / reinstall** — Android security forces re-grant of accessibility permission; documented in install instructions.
- **User disables accessibility service** — keypad reverts to original Latin; toggle no longer works; no data corruption possible.
- **Scanner trigger** — Urovo scan events bypass `onKeyEvent` entirely (handled by `ScanManager` SDK). Safe by default.

## 9. Testing

**Unit tests (pure JVM):**
- For every (KEYCODE × metaState) combination in §7, assert correct `Result`. ~120 cases.
- `ModeStore` round-trip.
- Toggle rate-limit logic.

**Instrumented tests (run on device via `adb`):**
- Toggle EN→RU; type a-z (skipping q/w/x) → expect `абцдефгхийклмнопрстющвуяз`.
- Type with Shift held → all capitals.
- Type with Ctrl held → expected extras at expected keys.
- Pass-through cases: digits, arrows, Backspace, Enter, scan emit unchanged in both modes.
- Boot scenario: simulate `BOOT_COMPLETED` → mode is EN.
- Service kill + restart: mode persists from `SharedPreferences`.

**Manual smoke test on the actual RT40S device, in:**
- Stock Android text fields (Settings search, Chrome address bar, Messages).
- A representative WebView (Chrome page with `<input>`).
- Whichever warehouse apps the team installs on the device — universal-app support is a hard requirement, so any app that fails on first install must be triaged and the failing tier determined from `logcat`.

Per the universal-coverage requirement, we test enough generic surface that both injection tiers are exercised.

## 10. Build & artifacts

- Standard Android Studio project, Gradle build.
- `minSdk = 28` (Android 9), `targetSdk = 34` (Android 14). Device runs Android 13 — comfortably in range.
- Output: `cyr-toggle-1.0.0-release.apk`, signed with a stable keystore (so updates can be applied without uninstall).
- APK size target: ≤ 250 KB.
- No third-party dependencies. Pure AndroidX.

## 11. Worker-facing install (summary — full instructions delivered separately)

1. Copy `cyr-toggle-1.0.0-release.apk` to the device (USB cable, file share, or MDM push).
2. Open file manager on device → tap APK → allow "install from unknown sources" if prompted.
3. Open **Settings → Accessibility → Cyrillic Toggle → enable**.
4. Done. **Press Shift + Space anywhere to switch between EN and RU.** A short toast shows the current mode.

## 12. Open items

None blocking. The following are deferred to implementation:

- Final keystore generation procedure (debug vs. corporate-signed).
- Whether a tiny vibration on q/w/x silent-consume helps users vs. annoys them — to be evaluated during user trial.
- Whether to add a "force EN" hardware-button shortcut for emergency reset (e.g., long-press P2) — only if real-world use surfaces a need.
