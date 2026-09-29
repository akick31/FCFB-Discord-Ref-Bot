package com.fcfb.discord.refbot.config.server

import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GamePayloadDeserializationTest {
    private val gson: Gson = GsonBuilder().configureForGamePayloads().create()

    private fun gameJson(gameModeSetAt: String) =
        """
        {
          "game_id": 1,
          "home_platform_id": "123456789",
          "game_status": "OPENING_KICKOFF",
          "game_mode": "CHEW",
          "game_mode_set_by": "admin",
          "game_mode_set_at": $gameModeSetAt
        }
        """.trimIndent()

    @Test
    fun `parses a game mode payload whose set_at is an ISO-8601 string`() {
        val game = gson.fromJson(gameJson("\"2026-09-15T21:24:59\""), Game::class.java)

        assertEquals(GameMode.CHEW, game.gameMode)
        assertEquals("2026-09-15T21:24:59", game.gameModeSetAt)
    }

    @Test
    fun `rejects a game mode payload whose set_at is a timestamp array`() {
        assertFailsWith<Exception> {
            gson.fromJson(gameJson("[2026,9,15,21,24,59]"), Game::class.java)
        }
    }

    @Test
    fun `parses a game mode payload with a null set_at`() {
        val game = gson.fromJson(gameJson("null"), Game::class.java)

        assertEquals(GameMode.CHEW, game.gameMode)
        assertNull(game.gameModeSetAt)
    }
}
