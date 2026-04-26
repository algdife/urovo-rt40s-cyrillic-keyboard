package ru.urovo.cyrtoggle

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View

/**
 * Transparent Input Method Service.
 *
 * - Has NO soft input view (`onCreateInputView` returns null).
 * - Hardware keys flow through Android's IME dispatch into `onKeyDown` here.
 * - In RU mode, letter keys (and comma/period) are translated via `KeyMap` and
 *   committed as Cyrillic via `InputConnection.commitText`, consuming the
 *   original keycode so the Latin character is not also typed.
 *
 * Mode toggle: **long-press Space** (held ≥ TOGGLE_HOLD_MS).
 *
 * Why long-press: the Urovo RT40S keypad firmware does not deliver true Shift
 * modifier KEYDOWN events to the IME (verified via logcat — only individual
 * letter keys arrive with their pre-applied meta state). The original
 * "Shift+Space chord" plan is physically un-detectable on this hardware.
 *
 * The Space key is implemented with a small buffer: on first DOWN we do NOT
 * type a space immediately; we wait for either UP (= short press, type one
 * space on UP) or for the held duration to exceed TOGGLE_HOLD_MS (= long
 * press, toggle mode and consume). This adds about one keystroke worth of
 * latency to typing a space, which is acceptable for warehouse data entry.
 */
class CyrToggleInputMethodService : InputMethodService() {

    private lateinit var modeStore: ModeStore
    private lateinit var toaster: Toaster

    // Long-press Space state.
    private var spaceDownAtMs: Long = 0L
    private var spaceLongPressFired: Boolean = false

    // Ctrl tracking. There's no Caps Lock on this keypad, so for capital
    // Cyrillic extras (Ё, Ж, Ч, Щ, Ш, Ы, Я, Э) we use a double-tap-Ctrl
    // gesture: tap Ctrl twice within DOUBLE_CTRL_MS, then press the letter.
    // Single Ctrl-recent keeps producing the lowercase extra as before.
    private var lastCtrlAtMs: Long = 0L
    private var ctrlDoubleArmed: Boolean = false

    override fun onCreate() {
        super.onCreate()
        modeStore = ModeStore(this)
        toaster = Toaster(this)
        Log.i(TAG, "IME created, mode=${modeStore.get()}")
    }

    /** No on-screen keyboard. */
    override fun onCreateInputView(): View? = null

    override fun onEvaluateInputViewShown(): Boolean = false

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyDown kc=$keyCode meta=${event.metaState} src=${event.source} dev=${event.deviceId} rep=${event.repeatCount}")

        // 0. Track Ctrl key presses. Double-tap Ctrl arms capital-extra mode.
        if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
            val now = SystemClock.uptimeMillis()
            val isDouble = (now - lastCtrlAtMs) in 1..DOUBLE_CTRL_MS
            if (isDouble) {
                ctrlDoubleArmed = true
                Log.i(TAG, "DOUBLE-CTRL armed — next letter -> capital extra")
            }
            lastCtrlAtMs = now
            return super.onKeyDown(keyCode, event)
        }

        // 1. Long-press-Space toggle.
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            return handleSpaceDown(event)
        }

        // 2. EN mode: commit the Latin char ourselves using a hardcoded map.
        // We avoid event.unicodeChar because the active KCM (the v2 "Russian
        // Phonetic (Urovo)" layout) leaks Cyrillic chars for unmodified keys
        // and only falls through to Latin for modified ones. Hardcoded map
        // gives stable EN behavior regardless of which KCM is selected.
        if (modeStore.get() == Mode.EN) {
            val ch = mapEnglish(keyCode, event.metaState)
            if (ch != null) {
                currentInputConnection?.commitText(ch.toString(), 1)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 3. RU mode: only letters + COMMA + PERIOD are candidates.
        if (!isMappableKeyCode(keyCode)) {
            return super.onKeyDown(keyCode, event)
        }

        // Apply Ctrl modifiers from gestures.
        // - ctrlDoubleArmed: double-tap-Ctrl-recent → force CTRL+SHIFT (capital extra).
        // - single sticky-Ctrl recent → force CTRL (lowercase extra).
        val now = SystemClock.uptimeMillis()
        val ctrlRecent = (now - lastCtrlAtMs) in 1..CTRL_STICKY_MS
        var metaMasked = event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_CAPS_LOCK_ON)
        if (ctrlDoubleArmed && ctrlRecent) {
            metaMasked = metaMasked or KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
            ctrlDoubleArmed = false
            lastCtrlAtMs = 0L
            Log.i(TAG, "double-ctrl consumed for kc=$keyCode -> capital extra")
        } else if (ctrlRecent) {
            metaMasked = metaMasked or KeyEvent.META_CTRL_ON
            lastCtrlAtMs = 0L
        }
        val lookup = KeyMap.lookup(keyCode, metaMasked)
        return when (lookup.result) {
            KeyMap.Result.PassThrough -> super.onKeyDown(keyCode, event)
            KeyMap.Result.Silent -> true   // consume, no output
            KeyMap.Result.Char -> {
                val char = lookup.char ?: return super.onKeyDown(keyCode, event)
                val ic = currentInputConnection
                if (ic == null) {
                    Log.w(TAG, "no InputConnection for char '$char'")
                    return true
                }
                val ok = ic.commitText(char.toString(), 1)
                if (!ok) Log.w(TAG, "commitText failed for char '$char'")
                true   // consume the original keycode
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            return handleSpaceUp()
        }

        // In RU mode, consume UP for any letter we translated on DOWN to keep
        // the event stream balanced.
        if (modeStore.get() == Mode.RU && isMappableKeyCode(keyCode)) {
            val metaMasked = event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON)
            val lookup = KeyMap.lookup(keyCode, metaMasked)
            if (lookup.result == KeyMap.Result.Char || lookup.result == KeyMap.Result.Silent) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Space DOWN handling — buffer the press, decide on UP whether it was a
     * short tap (type space) or a long press (toggle mode).
     */
    private fun handleSpaceDown(event: KeyEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        // Robust: set spaceDownAtMs on FIRST event we see (rep=0) OR if the
        // service binding missed rep=0 and we're now seeing a repeat without
        // a prior down recorded.
        if (event.repeatCount == 0 || spaceDownAtMs == 0L) {
            spaceDownAtMs = if (event.repeatCount == 0) now else now - (event.repeatCount * 50L)
            spaceLongPressFired = false
            return true
        }
        if (!spaceLongPressFired && (now - spaceDownAtMs) >= TOGGLE_HOLD_MS) {
            spaceLongPressFired = true
            val newMode = modeStore.toggle()
            toaster.show(newMode)
            Log.i(TAG, "long-press SPACE -> toggle -> $newMode (down-repeat)")
        }
        return true
    }

    private fun handleSpaceUp(): Boolean {
        val downAt = spaceDownAtMs
        val wasLong = spaceLongPressFired
        spaceDownAtMs = 0L
        spaceLongPressFired = false

        if (wasLong) {
            // Toggle already fired on DOWN — consume the UP, no space typed.
            return true
        }

        // Short tap — type one space.
        val now = SystemClock.uptimeMillis()
        if (downAt > 0 && (now - downAt) >= TOGGLE_HOLD_MS) {
            // Held long enough but the repeat events didn't fire (some
            // firmwares don't send key-repeat). Treat as long-press anyway.
            val newMode = modeStore.toggle()
            toaster.show(newMode)
            Log.i(TAG, "long-press SPACE (no-repeat fallback) -> toggle -> $newMode")
            return true
        }

        currentInputConnection?.commitText(" ", 1)
        return true
    }

    private fun isMappableKeyCode(kc: Int): Boolean = when (kc) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_PERIOD -> true
        else -> false
    }

    /**
     * Hardcoded English mapping for EN mode. Independent of the device's
     * currently-selected KCM. Returns null for non-printable / unknown
     * keycodes so the caller can fall back to super.onKeyDown.
     */
    private fun mapEnglish(keyCode: Int, metaState: Int): Char? {
        val shift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        return when (keyCode) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
                val lower = ('a' + (keyCode - KeyEvent.KEYCODE_A))
                if (shift) lower.uppercaseChar() else lower
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                val digit = ('0' + (keyCode - KeyEvent.KEYCODE_0))
                if (shift) shiftDigit(digit) else digit
            }
            KeyEvent.KEYCODE_COMMA  -> if (shift) '<' else ','
            KeyEvent.KEYCODE_PERIOD -> if (shift) '>' else '.'
            KeyEvent.KEYCODE_MINUS  -> if (shift) '_' else '-'
            KeyEvent.KEYCODE_EQUALS -> if (shift) '+' else '='
            KeyEvent.KEYCODE_SEMICOLON -> if (shift) ':' else ';'
            KeyEvent.KEYCODE_APOSTROPHE -> if (shift) '"' else '\''
            KeyEvent.KEYCODE_SLASH -> if (shift) '?' else '/'
            KeyEvent.KEYCODE_BACKSLASH -> if (shift) '|' else '\\'
            KeyEvent.KEYCODE_LEFT_BRACKET -> if (shift) '{' else '['
            KeyEvent.KEYCODE_RIGHT_BRACKET -> if (shift) '}' else ']'
            KeyEvent.KEYCODE_GRAVE -> if (shift) '~' else '`'
            else -> null
        }
    }

    private fun shiftDigit(d: Char): Char = when (d) {
        '1' -> '!'; '2' -> '@'; '3' -> '#'; '4' -> '$'; '5' -> '%'
        '6' -> '^'; '7' -> '&'; '8' -> '*'; '9' -> '('; '0' -> ')'
        else -> d
    }

    companion object {
        private const val TAG = "CyrToggle.IME"
        /** Minimum time the Space key must be held to count as a toggle gesture. */
        private const val TOGGLE_HOLD_MS = 400L
        /** Window after a bare Ctrl press during which a Shift+letter is treated as Ctrl+Shift+letter. */
        private const val CTRL_STICKY_MS = 400L
        /** Maximum gap between two Ctrl presses to count as a double-tap. */
        private const val DOUBLE_CTRL_MS = 400L
    }
}
