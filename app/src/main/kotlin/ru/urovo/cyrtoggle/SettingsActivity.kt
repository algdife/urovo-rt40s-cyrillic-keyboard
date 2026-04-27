package ru.urovo.cyrtoggle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.enableImeButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.switchImeButton).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        findViewById<Button>(R.id.softKbdToggleButton).setOnClickListener {
            val store = ModeStore(this)
            val newVal = !store.isSoftKbdEnabled()
            store.setSoftKbdEnabled(newVal)
            refreshSoftKbdButton()
        }

        findViewById<Button>(R.id.openSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = when {
            isImeActive() -> getString(R.string.status_ime_active)
            isImeEnabled() -> getString(R.string.status_ime_enabled_inactive)
            else -> getString(R.string.status_ime_disabled)
        }
        refreshSoftKbdButton()
    }

    private fun refreshSoftKbdButton() {
        val btn = findViewById<Button>(R.id.softKbdToggleButton)
        val on = ModeStore(this).isSoftKbdEnabled()
        btn.text = getString(if (on) R.string.soft_kbd_on else R.string.soft_kbd_off)
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any {
            it.packageName == packageName &&
                    it.serviceName == CyrToggleInputMethodService::class.java.name
        }
    }

    private fun isImeActive(): Boolean {
        val activeId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        return activeId.startsWith("$packageName/")
                && activeId.endsWith(CyrToggleInputMethodService::class.java.name)
    }
}
