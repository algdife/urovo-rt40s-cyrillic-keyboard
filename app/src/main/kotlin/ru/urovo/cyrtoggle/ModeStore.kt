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
