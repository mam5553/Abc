package com.abc.tvlauncher.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.abc.tvlauncher.LauncherApp
import com.abc.tvlauncher.MainActivity
import com.abc.tvlauncher.data.AppRepository

/**
 * Optional helper so the remote's Home button opens this launcher instead of the stock home screen.
 *
 * How: when the stock home app's window appears, we immediately bring this launcher to the front
 * (the same trick Projectivy Launcher uses). We read nothing else: no screen content, no text.
 * The service only reacts to windows of apps that declare themselves a Home screen.
 */
class HomeKeyService : AccessibilityService() {
    private var homePackages: Set<String> = emptySet()
    private var homePackagesAt = 0L
    private var lastLaunch = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || !takeoverOn()) return
        if (pkg in stockHomes()) openOurHome()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Most TVs never deliver KEYCODE_HOME here, but if one does we handle it directly.
        if (event.keyCode == KeyEvent.KEYCODE_HOME && takeoverOn()) {
            if (event.action == KeyEvent.ACTION_UP) openOurHome()
            return true
        }
        return false
    }

    override fun onInterrupt() = Unit

    private fun takeoverOn() = LauncherApp.model.homeTakeover

    private fun stockHomes(): Set<String> {
        val now = SystemClock.elapsedRealtime()
        if (now - homePackagesAt > 30_000) {
            homePackages = runCatching { AppRepository(this).otherHomePackages() }.getOrDefault(homePackages)
            homePackagesAt = now
        }
        return homePackages
    }

    private fun openOurHome() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 700) return
        lastLaunch = now
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { startActivity(i) }
    }
}
