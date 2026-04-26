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
                Log.i(TAG, "toggle -> $newMode")
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
