package com.abc.tvlauncher.data

import android.content.ComponentName

/** An app that can be launched from the home screen. */
data class AppEntry(
    val pkg: String,
    val label: String,
    val component: ComponentName,
    val updated: Long,
)

/** Any installed package, for the Optimize screen. */
data class PkgInfo(
    val pkg: String,
    val label: String,
    val preinstalled: Boolean,
    val enabled: Boolean,
    val hasLauncher: Boolean,
    val updated: Long,
    val note: BloatNote?,
) {
    val risk: Risk get() = note?.risk ?: Risk.NONE
    val knownBloat: Boolean get() = Bloat.isKnownBloat(pkg)
}

enum class GameState { PRE, LIVE, POST }

data class Team(val abbr: String, val name: String, val logo: String?, val score: Int?)

data class Game(
    val id: String,
    val league: String,
    val away: Team,
    val home: Team,
    val state: GameState,
    /** Period/clock for live games, "Final" for finished ones, empty for upcoming. */
    val statusText: String,
    val startMillis: Long,
)
