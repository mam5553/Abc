package com.abc.tvlauncher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abc.tvlauncher.LauncherApp

/** After a reboot the next time the launcher opens counts as "TV just woke up". */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LauncherApp.model.markWoke()
    }
}
