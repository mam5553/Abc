package com.abc.tvlauncher.data

/** How safe it is to disable something. Shown as a coloured chip on the Optimize screen. */
enum class Risk(val label: String) {
    /** Core system piece - disabling can break the TV. */
    CORE("CORE - don't touch"),
    /** Preinstalled and optional, but check what it does first. */
    CAREFUL("Careful"),
    /** Commonly disabled with no real downside. */
    SAFE("Usually safe"),
    NONE("")
}

data class BloatNote(val risk: Risk, val note: String)

/**
 * Hand-picked notes about common Google TV / Android TV / Sony system packages.
 * This is a guide, not gospel - package names vary by model and firmware.
 * Add entries freely; unknown preinstalled apps are still marked "Preinstalled".
 */
object Bloat {
    private val exact: Map<String, BloatNote> = mapOf(
        // --- Usually safe to disable / force-stop ---
        "com.google.android.videos" to BloatNote(Risk.SAFE, "Google TV movies & shows store"),
        "com.google.android.youtube.tvmusic" to BloatNote(Risk.SAFE, "YouTube Music"),
        "com.google.android.play.games" to BloatNote(Risk.SAFE, "Play Games"),
        "com.google.android.backdrop" to BloatNote(Risk.SAFE, "Ambient screensaver"),
        "com.google.android.apps.tv.dreamx" to BloatNote(Risk.SAFE, "Google TV ambient mode / screensaver"),
        "com.google.android.leanbacklauncher.recommendations" to BloatNote(Risk.SAFE, "Old home-screen recommendations service"),
        "com.google.android.tvrecommendations" to BloatNote(Risk.CAREFUL, "Feeds recommendations to the stock home screen. Fine to disable if you only use this launcher"),
        "com.google.android.feedback" to BloatNote(Risk.SAFE, "Feedback reporter"),
        "com.google.android.tv.bugreportsender" to BloatNote(Risk.SAFE, "Bug report sender"),
        "com.google.android.partnersetup" to BloatNote(Risk.SAFE, "Setup helper, only used during first setup"),
        "com.google.android.onetimeinitializer" to BloatNote(Risk.SAFE, "One-time setup helper"),
        "com.android.printspooler" to BloatNote(Risk.SAFE, "Printing"),
        "com.android.bips" to BloatNote(Risk.SAFE, "Printing"),
        // --- Careful ---
        "com.google.android.apps.mediashell" to BloatNote(Risk.CAREFUL, "Chromecast built-in. Disable only if you never cast"),
        "com.google.android.katniss" to BloatNote(Risk.CAREFUL, "Google Assistant / voice search"),
        "com.google.android.youtube.tv" to BloatNote(Risk.CAREFUL, "YouTube app"),
        "com.google.android.tvlauncher" to BloatNote(Risk.CAREFUL, "Stock Android TV home screen. Keep it unless this launcher is your default"),
        "com.google.android.apps.tv.launcherx" to BloatNote(Risk.CAREFUL, "Stock Google TV home screen. Keep it unless this launcher is your default"),
        // --- Core ---
        "android" to BloatNote(Risk.CORE, "Android system"),
        "com.android.systemui" to BloatNote(Risk.CORE, "System UI"),
        "com.android.tv.settings" to BloatNote(Risk.CORE, "TV Settings"),
        "com.google.android.gms" to BloatNote(Risk.CORE, "Google Play services"),
        "com.google.android.gsf" to BloatNote(Risk.CORE, "Google services framework"),
        "com.android.vending" to BloatNote(Risk.CORE, "Google Play Store"),
        "com.google.android.tv.remote.service" to BloatNote(Risk.CORE, "Remote control service"),
        "com.android.providers.settings" to BloatNote(Risk.CORE, "Settings storage"),
        "com.android.providers.media" to BloatNote(Risk.CORE, "Media storage"),
        "com.android.providers.tv" to BloatNote(Risk.CORE, "TV provider (inputs/channels)"),
        "com.android.packageinstaller" to BloatNote(Risk.CORE, "Installs apps"),
        "com.google.android.packageinstaller" to BloatNote(Risk.CORE, "Installs apps"),
        "com.android.inputmethod.latin" to BloatNote(Risk.CORE, "Keyboard"),
        "com.google.android.inputmethod.latin" to BloatNote(Risk.CORE, "Keyboard"),
        "com.android.shell" to BloatNote(Risk.CORE, "Shell"),
    )

    private val prefixes: List<Pair<String, BloatNote>> = listOf(
        "com.android.providers." to BloatNote(Risk.CORE, "System data provider"),
        "com.sony." to BloatNote(Risk.CAREFUL, "Sony system app - some run TV features (inputs, picture, remote). Disable only if you know what it does"),
    )

    fun noteFor(pkg: String): BloatNote? =
        exact[pkg] ?: prefixes.firstOrNull { pkg.startsWith(it.first) }?.second

    /** Packages whose risk is [Risk.SAFE] are the "bloat" the Optimize screen highlights. */
    fun isKnownBloat(pkg: String) = exact[pkg]?.risk == Risk.SAFE
}
