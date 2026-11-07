package com.abc.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abc.tvlauncher.ui.Root
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model get() = LauncherApp.model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root(model) }

        // Only runs while the launcher is on screen: score refresh every 60s, clock tick every 30s.
        // Nothing is fetched, preloaded or kept alive while another app is in front.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { while (true) { model.refreshGames(); delay(60_000) } }
                launch { while (true) { model.tickClock(); delay(30_000) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while we're already running.
        model.onHomePressed()
    }

    override fun onResume() {
        super.onResume()
        model.maybeStartIntro()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (model.activeIntro != null) {            // any button skips the intro
            if (event.action == KeyEvent.ACTION_DOWN) model.skipIntro()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
