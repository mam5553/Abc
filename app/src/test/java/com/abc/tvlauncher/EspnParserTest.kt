package com.abc.tvlauncher

import com.abc.tvlauncher.data.Espn
import com.abc.tvlauncher.data.GameState
import com.abc.tvlauncher.data.League
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class EspnParserTest {
    private val nba = League("NBA", "basketball/nba")
    private val json = javaClass.getResource("/nba_sample.json")!!.readText()
    private val et = ZoneId.of("America/New_York")

    private fun at(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()

    @Test fun keepsOnlyAtlantaGameFromToday() {
        // Real response: Hawks 139 - Wizards 132, final, started 2026-11-07T00:00Z = Nov 6, 7pm Eastern
        val games = Espn.parse(json, nba, at("2026-11-06T23:00:00-05:00[America/New_York]"), et)
        assertEquals(1, games.size)
        val g = games[0]
        assertEquals(GameState.POST, g.state)
        assertEquals("ATL", g.home.abbr)
        assertEquals(139, g.home.score)
        assertEquals(132, g.away.score)
        assertTrue(g.statusText.startsWith("Final"))
    }

    @Test fun hidesYesterdaysFinishedGame() {
        val games = Espn.parse(json, nba, at("2026-11-07T10:00:00-05:00[America/New_York]"), et)
        assertTrue(games.isEmpty())
    }

    @Test fun otherLeagueCityIsIgnored() {
        val games = Espn.parse(json, League("NBA", "x", teamCity = "Gotham"), at("2026-11-06T23:00:00-05:00[America/New_York]"), et)
        assertTrue(games.isEmpty())
    }

    @Test fun emptyScoreboard() {
        assertTrue(Espn.parse("""{"events":[]}""", nba, 0L, et).isEmpty())
        assertTrue(Espn.parse("""{}""", nba, 0L, et).isEmpty())
    }
}
