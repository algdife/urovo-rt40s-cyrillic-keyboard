package ru.urovo.cyrkcm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.grantOverlayButton).setOnClickListener {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(i)
        }

        findViewById<Button>(R.id.startToggleButton).setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, ToggleOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }

        findViewById<Button>(R.id.stopToggleButton).setOnClickListener {
            stopService(Intent(this, ToggleOverlayService::class.java))
        }

        findViewById<Button>(R.id.openKbdSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = if (Settings.canDrawOverlays(this)) {
            getString(R.string.status_overlay_granted)
        } else {
            getString(R.string.status_overlay_missing)
        }
    }
}
