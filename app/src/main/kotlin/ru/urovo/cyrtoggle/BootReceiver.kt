package ru.urovo.cyrtoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            ModeStore(context).set(Mode.EN)
            Log.i("CyrToggle.Boot", "mode reset to EN")
        }
    }
}
