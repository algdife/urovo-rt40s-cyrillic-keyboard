package ru.urovo.cyrtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Test/diagnostic broadcast receiver. Trigger from adb:
 *   adb shell am broadcast -a ru.urovo.cyrtoggle.TOGGLE
 *   adb shell am broadcast -a ru.urovo.cyrtoggle.SET --es mode RU
 *   adb shell am broadcast -a ru.urovo.cyrtoggle.SET --es mode EN
 *
 * Used to flip mode without hardware Shift+Space, so we can self-test from
 * the dev workstation.
 */
class ToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = ModeStore(context)
        when (intent.action) {
            ACTION_TOGGLE -> {
                val newMode = store.toggle()
                Log.i(TAG, "TOGGLE -> $newMode")
                Toaster(context).show(newMode)
            }
            ACTION_SET -> {
                val modeStr = intent.getStringExtra("mode")
                val newMode = try { Mode.valueOf(modeStr ?: "EN") } catch (e: Exception) { Mode.EN }
                store.set(newMode)
                Log.i(TAG, "SET -> $newMode")
            }
            ACTION_SOFT_KBD -> {
                val newVal = !store.isSoftKbdEnabled()
                store.setSoftKbdEnabled(newVal)
                Log.i(TAG, "SOFT_KBD -> $newVal")
                Toaster(context).show(store.get())
            }
        }
    }

    companion object {
        private const val TAG = "CyrToggle.Receiver"
        const val ACTION_TOGGLE = "ru.urovo.cyrtoggle.TOGGLE"
        const val ACTION_SET = "ru.urovo.cyrtoggle.SET"
        const val ACTION_SOFT_KBD = "ru.urovo.cyrtoggle.SOFT_KBD"
    }
}
