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
