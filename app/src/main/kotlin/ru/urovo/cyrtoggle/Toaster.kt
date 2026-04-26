package ru.urovo.cyrtoggle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast

class Toaster(private val context: Context) {

    private var lastToast: Toast? = null
    private val nm: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Cyrillic mode", NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    fun show(mode: Mode) {
        // 1. Toast — large, long, always-fired.
        lastToast?.cancel()
        val text = when (mode) {
            Mode.EN -> "  EN  "
            Mode.RU -> "  РУ  "
        }
        lastToast = Toast.makeText(context, text, Toast.LENGTH_LONG).also { it.show() }

        // 2. Persistent notification — always visible in status bar.
        val notif = Notification.Builder(
            context,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CHANNEL else ""
        )
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Cyrillic Toggle: ${if (mode == Mode.RU) "РУ" else "EN"}")
            .setContentText("Mode is ${mode.name} — long-press Space to toggle")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    companion object {
        private const val CHANNEL = "cyr_mode"
        private const val NOTIF_ID = 42
    }
}
