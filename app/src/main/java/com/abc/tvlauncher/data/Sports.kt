package com.abc.tvlauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** One ESPN scoreboard to check. Add a line to [Espn.leagues] to follow another team/league. */
data class League(val code: String, val path: String, val teamCity: String = "Atlanta")

object Espn {
    val leagues = listOf(
        League("NBA", "basketball/nba"),    // Hawks
        League("NFL", "football/nfl"),      // Falcons
        League("MLB", "baseball/mlb"),      // Braves
        League("MLS", "soccer/usa.1"),      // Atlanta United
        League("WNBA", "basketball/wnba"),  // Dream
    )

    fun url(l: League) = "https://site.api.espn.com/apis/site/v2/sports/${l.path}/scoreboard"

    /**
     * Parses an ESPN scoreboard response, keeping only [League.teamCity] games that are live
     * or start today in [zone].
     */
    fun parse(json: String, league: League, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<Game> {
        val events = JSONObject(json).optJSONArray("events") ?: return emptyList()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val out = ArrayList<Game>()
        for (i in 0 until events.length()) {
            val e = events.optJSONObject(i) ?: continue
            val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: continue
            val comps = comp.optJSONArray("competitors") ?: continue
            var home: Team? = null
            var away: Team? = null
            var ours = false
            for (j in 0 until comps.length()) {
                val c = comps.optJSONObject(j) ?: continue
                val t = c.optJSONObject("team") ?: continue
                val team = Team(
                    abbr = t.optString("abbreviation"),
                    name = t.optString("shortDisplayName", t.optString("displayName")),
                    logo = t.optString("logo").takeIf { it.isNotBlank() },
                    score = c.optString("score").toIntOrNull(),
                )
                val city = t.optString("location")
                if (city == league.teamCity || t.optString("displayName").startsWith(league.teamCity)) ours = true
                if (c.optString("homeAway") == "home") home = team else away = team
            }
            if (!ours || home == null || away == null) continue

            val start = runCatching { OffsetDateTime.parse(e.optString("date")).toInstant().toEpochMilli() }.getOrDefault(0L)
            val type = e.optJSONObject("status")?.optJSONObject("type")
            val state = when (type?.optString("state")) {
                "in" -> GameState.LIVE
                "post" -> GameState.POST
                else -> GameState.PRE
            }
            val startDay = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            if (state != GameState.LIVE && startDay != today) continue
            if (state == GameState.POST && type?.optString("completed") == "false") continue

            val text = when (state) {
                GameState.LIVE, GameState.POST -> type?.optString("shortDetail").orEmpty()
                GameState.PRE -> ""
            }
            out += Game(e.optString("id"), league.code, away, home, state, text, start)
        }
        return out
    }

    fun sorted(games: List<Game>): List<Game> = games.sortedWith(
        compareBy<Game>({ if (it.state == GameState.LIVE) 0 else if (it.state == GameState.PRE) 1 else 2 }, { it.startMillis })
    )
}

/** Fetches all Atlanta teams in parallel. A league that fails keeps its previous games. */
class SportsRepository {
    private val last = HashMap<String, List<Game>>()

    suspend fun refresh(now: Long = System.currentTimeMillis()): List<Game> = coroutineScope {
        Espn.leagues.map { l ->
            async(Dispatchers.IO) {
                val result = runCatching { Espn.parse(httpGet(Espn.url(l)), l, now) }
                synchronized(last) { result.getOrNull()?.let { last[l.code] = it } }
            }
        }.awaitAll()
        withContext(Dispatchers.Default) {
            Espn.sorted(synchronized(last) { last.values.flatten() })
        }
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "AbcLauncher/1.0")
        return try {
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
