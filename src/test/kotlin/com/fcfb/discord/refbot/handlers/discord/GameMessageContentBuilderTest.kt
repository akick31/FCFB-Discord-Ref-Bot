package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.GameWriteupClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.config.server.configureForGamePayloads
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.game.GameDescriptionUtils
import com.fcfb.discord.refbot.utils.game.GameParsingUtils
import com.google.gson.GsonBuilder
import dev.kord.common.entity.optional.value
import dev.kord.core.Kord
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameMessageContentBuilderTest {
    private val gson = GsonBuilder().configureForGamePayloads().create()
    private val gameParsingUtils = mockk<GameParsingUtils> { every { isKickoff(any()) } returns false }
    private val gameDescriptionUtils = mockk<GameDescriptionUtils>(relaxed = true)
    private val gameWriteupClient = mockk<GameWriteupClient>()
    private val scorebugClient = mockk<ScorebugClient>()
    private val fcfbUserClient = mockk<FCFBUserClient>(relaxed = true)
    private val client = mockk<Kord>()

    private val builder =
        GameMessageContentBuilder(gameParsingUtils, gameDescriptionUtils, gameWriteupClient, scorebugClient, fcfbUserClient)

    private fun game(gameMode: String): Game =
        gson.fromJson(
            """
            {
              "game_id": 1,
              "home_team": "Coastal Carolina",
              "away_team": "Delaware",
              "home_coach_discord_ids": [],
              "away_coach_discord_ids": [],
              "home_score": 7,
              "away_score": 3,
              "possession": "HOME",
              "game_mode": "$gameMode",
              "game_status": "IN_PROGRESS"
            }
            """.trimIndent(),
            Game::class.java,
        )

    private val passPlay: Play =
        gson.fromJson(
            """{ "play_id": 1, "game_id": 1, "possession": "HOME", "play_call": "PASS" }""",
            Play::class.java,
        )

    private fun stubPlayResultWriteups() {
        coEvery {
            gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, PlayCall.PASS)
        } returns mapOf("Coastal Carolina with a clean throw for 13 yards." to null)
        coEvery {
            gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null)
        } returns mapOf("{play_writeup}<br><br>It's 1st & 10." to null)
        coEvery { scorebugClient.getScorebugByGameId(any()) } returns null
    }

    @Test
    fun `prepends the chew mode header before a play result when the game is in chew mode`() =
        runBlocking {
            stubPlayResultWriteups()

            val (message, _) = builder.createGameMessage(client, game("CHEW"), Scenario.PLAY_RESULT, passPlay)
            val description = message.second?.description?.value

            assertNotNull(description)
            assertTrue(
                description.startsWith("The game is currently in chew mode\n\nCoastal Carolina with a clean throw for 13 yards."),
                "Chew mode play result should lead with the header, then the play writeup. Was:\n$description",
            )
        }

    @Test
    fun `does not prepend the chew mode header for a normal game`() =
        runBlocking {
            stubPlayResultWriteups()

            val (message, _) = builder.createGameMessage(client, game("NORMAL"), Scenario.PLAY_RESULT, passPlay)
            val description = message.second?.description?.value

            assertNotNull(description)
            assertFalse(description.contains("chew mode"), "Normal games should not mention chew mode. Was:\n$description")
        }

    private fun stubNumberRequestWriteup() {
        coEvery {
            gameWriteupClient.getGameMessageByScenario(Scenario.NORMAL_NUMBER_REQUEST, null)
        } returns mapOf("Please submit a number between 1 and 1500 (inclusive){game_status}" to null)
        coEvery { scorebugClient.getScorebugByGameId(any()) } returns null
    }

    @Test
    fun `bolds the chew notice right after the number prompt on a chew mode number request`() =
        runBlocking {
            stubNumberRequestWriteup()

            val (message, _) = builder.createGameMessage(client, game("CHEW"), Scenario.NORMAL_NUMBER_REQUEST, null)
            val description = message.second?.description?.value

            assertNotNull(description)
            assertTrue(
                description.contains("a number between 1 and 1500 (inclusive)\n\n**The game is in chew mode**"),
                "Chew notice should be bold and immediately follow the number prompt. Was:\n$description",
            )
        }

    @Test
    fun `does not add the chew notice on a number request for a normal game`() =
        runBlocking {
            stubNumberRequestWriteup()

            val (message, _) = builder.createGameMessage(client, game("NORMAL"), Scenario.NORMAL_NUMBER_REQUEST, null)
            val description = message.second?.description?.value

            assertNotNull(description)
            assertFalse(description.contains("chew mode"), "Normal games should not mention chew mode. Was:\n$description")
        }
}
