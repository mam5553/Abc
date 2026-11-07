package com.abc.tvlauncher

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.abc.tvlauncher.data.AppEntry
import com.abc.tvlauncher.data.AppOrdering
import com.abc.tvlauncher.data.AppRepository
import com.abc.tvlauncher.data.Game
import com.abc.tvlauncher.data.IconCache
import com.abc.tvlauncher.data.SportsRepository
import com.abc.tvlauncher.system.Actions
import com.abc.tvlauncher.ui.intro.Intro
import com.abc.tvlauncher.ui.intro.Intros
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Optimize : Screen
    data object SportsAppPicker : Screen
    data object XboxAppPicker : Screen
    data object HdmiPicker : Screen
    data object HiddenApps : Screen
}

enum class XboxMode { HDMI, APP }

/**
 * Single source of truth for the launcher. Lives in [LauncherApp] so it survives the activity
 * being recreated. All UI state is Compose snapshot state; everything user-chosen is persisted
 * in SharedPreferences.
 */
class LauncherModel(private val app: Application) {
    private val prefs = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repo = AppRepository(app)
    private val sports = SportsRepository()

    // ---- navigation / live data -------------------------------------------------------------
    var screen by mutableStateOf<Screen>(Screen.Home)
    var homeTick by mutableIntStateOf(0)            // bumps on every Home press (scroll to top)
    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var games by mutableStateOf<List<Game>>(emptyList()); private set
    var now by mutableLongStateOf(System.currentTimeMillis()); private set

    // ---- settings ---------------------------------------------------------------------------
    var introEnabled by mutableStateOf(prefs.getBoolean("intro", false)); private set
    var sportsPkg by mutableStateOf(prefs.getString("sportsPkg", null)); private set
    var xboxMode by mutableStateOf(runCatching { XboxMode.valueOf(prefs.getString("xboxMode", "HDMI")!!) }.getOrDefault(XboxMode.HDMI)); private set
    var hdmiInputId by mutableStateOf(prefs.getString("hdmiInput", null)); private set
    var xboxPkg by mutableStateOf(prefs.getString("xboxPkg", null)); private set
    var homeTakeover by mutableStateOf(prefs.getBoolean("takeover", true)); private set
    var checklistDone by mutableStateOf(prefs.getStringSet("checklist", emptySet())!!.toSet()); private set

    // ---- app layout -------------------------------------------------------------------------
    private var order by mutableStateOf(prefs.getString("order", "")!!.split('\n').filter { it.isNotEmpty() })
    var pinned by mutableStateOf(prefs.getStringSet("pinned", emptySet())!!.toSet()); private set
    var hidden by mutableStateOf(prefs.getStringSet("hidden", emptySet())!!.toSet()); private set
    var movingPkg by mutableStateOf<String?>(null)

    val visibleApps: List<AppEntry> by derivedStateOf {
        val byPkg = apps.associateBy { it.pkg }
        AppOrdering.visible(apps.map { it.pkg }, order, pinned, hidden).mapNotNull { byPkg[it] }
    }
    val hiddenApps: List<AppEntry> by derivedStateOf { apps.filter { it.pkg in hidden } }

    // ---- intro ------------------------------------------------------------------------------
    var activeIntro by mutableStateOf<Intro?>(null); private set
    private var introPending = prefs.getBoolean("wokePending", false)
    private var introStartedAt = 0L

    // =========================================================================================
    // Apps
    // =========================================================================================
    private var appJob: Job? = null

    fun refreshApps() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { repo.loadLaunchable() }
            apps = list
            withContext(Dispatchers.IO) { IconCache.prune(app, list) }
        }
    }

    /** Package installs come in bursts; wait a moment and refresh once. */
    fun scheduleAppRefresh() {
        appJob?.cancel()
        appJob = scope.launch { delay(600); refreshApps() }
    }

    fun togglePin(pkg: String) {
        if (pkg in pinned) {
            pinned = pinned - pkg
        } else {
            pinned = pinned + pkg
            order = listOf(pkg) + order.filter { it != pkg }   // newest pin goes first
        }
        savePins(); saveOrder()
    }

    fun hide(pkg: String) {
        hidden = hidden + pkg; pinned = pinned - pkg
        prefs.edit().putStringSet("hidden", hidden).putStringSet("pinned", pinned).apply()
    }

    fun unhide(pkg: String) {
        hidden = hidden - pkg
        prefs.edit().putStringSet("hidden", hidden).apply()
    }

    fun move(pkg: String, delta: Int) {
        val vis = visibleApps.map { it.pkg }
        val moved = AppOrdering.move(vis, pkg, delta, pinned)
        if (moved == vis) return
        order = moved + order.filter { it !in moved }
        saveOrder()
    }

    fun resetLayout() {
        order = emptyList(); pinned = emptySet(); hidden = emptySet()
        saveOrder(); savePins()
        prefs.edit().putStringSet("hidden", hidden).apply()
    }

    private fun saveOrder() = prefs.edit().putString("order", order.joinToString("\n")).apply()
    private fun savePins() = prefs.edit().putStringSet("pinned", pinned).apply()

    // =========================================================================================
    // Live data
    // =========================================================================================
    suspend fun refreshGames() {
        now = System.currentTimeMillis()
        runCatching { sports.refresh(now) }.getOrNull()?.let { games = it }
    }

    /** Test hook: show fixed data without touching the system. */
    internal fun setDemoData(apps: List<AppEntry>, games: List<Game>) { this.apps = apps; this.games = games }

    fun tickClock() { now = System.currentTimeMillis() }

    // =========================================================================================
    // Settings + actions
    // =========================================================================================
    fun updateIntroEnabled(v: Boolean) { introEnabled = v; prefs.edit().putBoolean("intro", v).apply() }
    fun updateSportsPkg(v: String?) { sportsPkg = v; prefs.edit().putString("sportsPkg", v).apply() }
    fun updateXboxMode(v: XboxMode) { xboxMode = v; prefs.edit().putString("xboxMode", v.name).apply() }
    fun setHdmiInput(v: String?) { hdmiInputId = v; prefs.edit().putString("hdmiInput", v).apply() }
    fun updateXboxPkg(v: String?) { xboxPkg = v; prefs.edit().putString("xboxPkg", v).apply() }
    fun updateHomeTakeover(v: Boolean) { homeTakeover = v; prefs.edit().putBoolean("takeover", v).apply() }
    fun toggleChecklist(id: String) {
        checklistDone = if (id in checklistDone) checklistDone - id else checklistDone + id
        prefs.edit().putStringSet("checklist", checklistDone).apply()
    }

    fun toggleChecklistOn(id: String) { if (id !in checklistDone) toggleChecklist(id) }

    /** Explicit choice, else the first well-known sports app that is installed. */
    val sportsApp: AppEntry? get() {
        val installed = apps.associateBy { it.pkg }
        sportsPkg?.let { installed[it]?.let { app -> return app } }
        return SPORTS_CANDIDATES.firstNotNullOfOrNull { installed[it] }
    }

    val xboxApp: AppEntry? get() {
        val installed = apps.associateBy { it.pkg }
        xboxPkg?.let { installed[it]?.let { app -> return app } }
        return apps.firstOrNull { it.pkg.startsWith("com.microsoft.xbox") || it.pkg.startsWith("com.microsoft.xcloud") }
    }

    fun openSports(ctx: Context) {
        val a = sportsApp
        if (a == null) Actions.toast(ctx, "Pick your sports app in Settings") else Actions.launchApp(ctx, a.pkg)
    }

    fun openXbox(ctx: Context) {
        when (xboxMode) {
            XboxMode.HDMI -> Actions.switchToHdmi(ctx, hdmiInputId)
            XboxMode.APP -> {
                val a = xboxApp
                if (a == null) Actions.toast(ctx, "Pick the Xbox app in Settings") else Actions.launchApp(ctx, a.pkg)
            }
        }
    }

    // =========================================================================================
    // Home key + intro
    // =========================================================================================
    fun onHomePressed() {
        screen = Screen.Home
        movingPkg = null
        homeTick++
    }

    /** The TV woke up (screen on after standby / boot): the next time we're shown, play an intro. */
    fun markWoke() {
        introPending = true
        prefs.edit().putBoolean("wokePending", true).apply()
    }

    fun maybeStartIntro() {
        if (!introPending) return
        introPending = false
        prefs.edit().putBoolean("wokePending", false).apply()
        if (!introEnabled) return
        screen = Screen.Home
        startIntro(Intros.pick(prefs.getString("lastIntro", null)))
    }

    fun startIntro(intro: Intro) {
        prefs.edit().putString("lastIntro", intro.id).apply()
        introStartedAt = SystemClock.uptimeMillis()
        activeIntro = intro
    }

    fun finishIntro() { activeIntro = null }

    /** Any button skips the intro (ignoring the first instant, so a stray key-up can't kill it). */
    fun skipIntro(): Boolean {
        if (activeIntro == null) return false
        if (SystemClock.uptimeMillis() - introStartedAt > 300) activeIntro = null
        return true
    }

    companion object {
        /** Tried in order when the user hasn't picked a sports app. */
        val SPORTS_CANDIDATES = listOf(
            "com.espn.score_center", "com.fubotv.android.tv", "com.google.android.youtube.unplugged",
            "com.amazon.amazonvideo.livingroom", "com.peacocktv.peacockandroid", "com.cbs.ott",
            "com.hulu.livingroomplus", "com.nbaimd.gametime.nba2011", "com.mlb.atbat.tv",
        )
    }
}
