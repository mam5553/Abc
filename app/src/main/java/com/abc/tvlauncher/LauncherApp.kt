package com.abc.tvlauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.ContextCompat

class LauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        model = LauncherModel(this)
        detectBoot()
        registerSystemReceivers()
        model.refreshApps()
    }

    /** A new boot (or first ever run) counts as the TV waking up. */
    private fun detectBoot() {
        val prefs = getSharedPreferences("launcher", Context.MODE_PRIVATE)
        val boot = Settings.Global.getInt(contentResolver, "boot_count", -1)
        if (boot != prefs.getInt("bootCount", Int.MIN_VALUE)) {
            prefs.edit().putInt("bootCount", boot).apply()
            model.markWoke()
        }
    }

    private fun registerSystemReceivers() {
        // Standby / screensaver => next launcher open plays the intro.
        val wake = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction("android.intent.action.DREAMING_STARTED")
        }
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = model.markWoke()
        }, wake, ContextCompat.RECEIVER_EXPORTED)

        // Installs / removals => refresh the app list.
        val pkgs = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = model.scheduleAppRefresh()
        }, pkgs, ContextCompat.RECEIVER_EXPORTED)
    }

    companion object {
        lateinit var model: LauncherModel
            private set
    }
}
