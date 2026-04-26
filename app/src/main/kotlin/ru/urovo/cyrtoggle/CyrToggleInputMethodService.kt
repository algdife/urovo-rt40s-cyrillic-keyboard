package ru.urovo.cyrtoggle

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View

/**
 * Transparent Input Method Service.
 *
 * - Has NO soft input view (`onCreateInputView` returns null) — when the user
 *   focuses a text field, no on-screen keyboard appears. The user types via
 *   the hardware keypad only.
 * - Hardware keys flow through Android's IME dispatch into `onKeyDown` here.
 * - In RU mode, letter keys are translated via `KeyMap` and committed as
 *   Cyrillic via `InputConnection.commitText`, consuming the original key
 *   so the Latin character is not also typed.
 * - Mode toggle on Shift+Space — but the Urovo keypad firmware *serializes*
 *   the chord (Space-down/up then Shift-down/up), so we use a small state
 *   machine: a Space tap that is followed by a Shift tap within 200 ms is
 *   treated as the toggle gesture.
 *
 * On EN mode: `onKeyDown` returns `super.onKeyDown` — pass-through.
 *
 * This service replaces `CyrToggleAccessibilityService` for the v3 path
 * needed on Urovo RT40S, where the vendor framework intercepts hardware
 * keys before any accessibility filter sees them.
 */
class CyrToggleInputMethodService : InputMethodService() {

    private lateinit var modeStore: ModeStore
    private lateinit var toaster: Toaster

    // State machine for the serialized Shift+Space toggle gesture.
    // The keypad firmware emits Space DOWN/UP, then Shift DOWN/UP — never as
    // a real chord. We watch for a recent Space tap and, if Shift follows
    // within TOGGLE_WINDOW_MS, treat it as the toggle.
    private var lastSpaceUpAtMs: Long = 0L
    private var lastToggleAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        modeStore = ModeStore(this)
        toaster = Toaster(this)
        Log.i(TAG, "IME created, mode=${modeStore.get()}")
    }

    /** No on-screen keyboard. */
    override fun onCreateInputView(): View? = null

    /** Don't show the IME view even on full-screen / extract mode. */
    override fun onEvaluateInputViewShown(): Boolean = false

    /** Don't go fullscreen for landscape extract-mode either. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyDown kc=$keyCode meta=${event.metaState} src=${event.source} dev=${event.deviceId}")
        // 1. Toggle state machine — Space tap immediately followed by Shift tap.
        if (handleToggleStateMachine(keyCode, event)) return true

        // 2. EN mode: pass everything through.
        if (modeStore.get() == Mode.EN) {
            return super.onKeyDown(keyCode, event)
        }

        // 3. RU mode: only letters + COMMA + PERIOD are candidates.
        if (!isMappableKeyCode(keyCode)) {
            return super.onKeyDown(keyCode, event)
        }

        val metaMasked = event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON)
        val lookup = KeyMap.lookup(keyCode, metaMasked)
        return when (lookup.result) {
            KeyMap.Result.PassThrough -> super.onKeyDown(keyCode, event)
            KeyMap.Result.Silent -> true   // consume, no output
            KeyMap.Result.Char -> {
                val char = lookup.char ?: return super.onKeyDown(keyCode, event)
                val ic = currentInputConnection
                if (ic == null) {
                    Log.w(TAG, "no InputConnection for char '$char'")
                    return true   // consume; if there's no IC, we can't fall back to Latin meaningfully
                }
                val ok = ic.commitText(char.toString(), 1)
                if (!ok) Log.w(TAG, "commitText failed for char '$char'")
                true   // consume the original keycode
            }
        }
    }

    /**
     * Detect the serialized Shift+Space toggle.
     *
     * Sequence we look for (firmware actually delivers this):
     *   KEY_SPACE down → KEY_SPACE up → KEY_LEFTSHIFT down (within 200 ms) → toggle.
     *
     * Returns `true` if this event was consumed as part of the toggle gesture.
     * Returns `false` if the event should fall through to normal handling.
     *
     * Note: the toggle "consumes" Shift-down. The user may notice that a Shift
     * keypress immediately after a Space is "swallowed" — acceptable trade-off
     * for getting any toggle at all on hardware that won't produce real chords.
     */
    private fun handleToggleStateMachine(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // We track Space-up timing in onKeyUp (see below). Space-down passes through.
            return false
        }

        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            val now = SystemClock.uptimeMillis()
            val sinceSpaceUp = now - lastSpaceUpAtMs
            if (sinceSpaceUp in 1..TOGGLE_WINDOW_MS) {
                // Rate-limit: don't toggle twice in rapid succession.
                if (now - lastToggleAtMs >= TOGGLE_RATE_LIMIT_MS) {
                    lastToggleAtMs = now
                    val newMode = modeStore.toggle()
                    toaster.show(newMode)
                    Log.i(TAG, "toggle -> $newMode (Space+Shift gesture)")
                }
                lastSpaceUpAtMs = 0L
                return true   // consume the Shift-down
            }
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Track Space-up so we can detect a following Shift within the toggle window.
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            lastSpaceUpAtMs = SystemClock.uptimeMillis()
        }
        // Consume Shift-up if it's part of the toggle gesture (we already toggled on down).
        if ((keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
            && SystemClock.uptimeMillis() - lastToggleAtMs < TOGGLE_RATE_LIMIT_MS
        ) {
            return true
        }
        // In RU mode, consume UP for any letter we translated on DOWN to keep the stream balanced.
        if (modeStore.get() == Mode.RU && isMappableKeyCode(keyCode)) {
            val metaMasked = event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON)
            val lookup = KeyMap.lookup(keyCode, metaMasked)
            if (lookup.result == KeyMap.Result.Char || lookup.result == KeyMap.Result.Silent) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isMappableKeyCode(kc: Int): Boolean = when (kc) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_PERIOD -> true
        else -> false
    }

    companion object {
        private const val TAG = "CyrToggle.IME"
        private const val TOGGLE_WINDOW_MS = 200L
        private const val TOGGLE_RATE_LIMIT_MS = 50L
    }
}
