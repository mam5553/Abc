package com.abc.tvlauncher

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.test.core.app.ApplicationProvider
import com.abc.tvlauncher.data.AppEntry
import com.abc.tvlauncher.data.Game
import com.abc.tvlauncher.data.GameState
import com.abc.tvlauncher.data.IconCache
import com.abc.tvlauncher.data.Team
import com.abc.tvlauncher.ui.HomeScreen
import com.abc.tvlauncher.ui.IntroStage
import com.abc.tvlauncher.ui.OptimizeScreen
import com.abc.tvlauncher.ui.SettingsScreen
import com.abc.tvlauncher.ui.intro.Intros
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real UI to PNGs (build/screenshots) so the look can be checked without a TV.
 * Not pixel-perfect vs. a device (no real fonts/blur), but catches broken layouts and drawings.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class ScreenshotTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val out = File(System.getProperty("screenshotDir") ?: "build/screenshots").apply { mkdirs() }

    private fun save(name: String) {
        val bmp = grab()
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun grab(): Bitmap {
        rule.waitForIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1920, 1080)
        view.draw(Canvas(bmp))
        return bmp
    }

    private fun fakeIcon(color: Int, letter: String) = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888).also { b ->
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color; c.drawRoundRect(RectF(8f, 8f, 184f, 184f), 40f, 40f, p)
        p.color = AColor.WHITE; p.textSize = 100f; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true
        c.drawText(letter, 96f, 130f, p)
    }.asImageBitmap()

    private fun model(): LauncherModel {
        val m = LauncherModel(ApplicationProvider.getApplicationContext<Application>())
        val names = listOf("Netflix", "YouTube", "Prime Video", "Disney+", "Max", "Spotify", "Plex", "Twitch",
            "ESPN", "Peacock", "Hulu", "Kodi", "Settings", "Files", "Browser", "Photos")
        val colors = listOf(0xFFE50914, 0xFFFF0000, 0xFF00A8E1, 0xFF113CCF, 0xFF5B2BE0, 0xFF1DB954, 0xFFE5A00D, 0xFF9146FF,
            0xFFD00000, 0xFF111111, 0xFF1CE783, 0xFF1F8AC0, 0xFF607D8B, 0xFF4CAF50, 0xFF2196F3, 0xFFFFB300)
        val apps = names.mapIndexed { i, n ->
            IconCache.put("pkg.$i", 1L, fakeIcon(colors[i].toInt(), n.take(1)))
            AppEntry("pkg.$i", n, android.content.ComponentName("pkg.$i", "A"), 1L)
        }
        fun team(a: String, n: String, s: Int?) = Team(a, n, null, s)
        val games = listOf(
            Game("1", "NBA", team("MIA", "Heat", 88), team("ATL", "Hawks", 92), GameState.LIVE, "3rd 4:12", 0),
            Game("2", "MLS", team("ORL", "Orlando City", null), team("ATL", "Atlanta United", null), GameState.PRE, "", System.currentTimeMillis() + 3600_000),
            Game("3", "WNBA", team("ATL", "Dream", 63), team("GS", "Valkyries", 68), GameState.POST, "Final", 0),
        )
        m.setDemoData(apps, games)
        return m
    }

    @Test fun home() {
        val m = model()
        rule.setContent { CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) { HomeScreen(m) } }
        rule.waitForIdle()
        save("home")
    }

    @Test fun settings() {
        val m = model()
        rule.setContent { SettingsScreen(m) }
        rule.waitForIdle()
        save("settings")
    }

    @Test fun optimize() {
        val m = model()
        rule.setContent { OptimizeScreen(m) }
        rule.waitForIdle()
        save("optimize")
    }

    @Test fun intros() {
        val m = model()
        var cur by androidx.compose.runtime.mutableStateOf<com.abc.tvlauncher.ui.intro.Intro?>(null)
        var tt by androidx.compose.runtime.mutableFloatStateOf(0f)
        rule.setContent { IntroStage(cur, { tt }) { HomeScreen(m) } }
        for (intro in Intros.all) {
            cur = intro
            val frames = 10
            val sheet = Bitmap.createBitmap(5 * 480, 2 * 270, Bitmap.Config.ARGB_8888)
            val sc = Canvas(sheet)
            for (step in 0 until frames) {
                tt = step / (frames - 1f) * 0.98f
                val bmp = grab()
                sc.drawBitmap(Bitmap.createScaledBitmap(bmp, 480, 270, true), (step % 5) * 480f, (step / 5) * 270f, null)
            }
            File(out, "intro_${intro.id}.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
