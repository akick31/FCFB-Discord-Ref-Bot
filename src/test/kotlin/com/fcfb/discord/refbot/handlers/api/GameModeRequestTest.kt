package com.fcfb.discord.refbot.handlers.api

import com.fcfb.discord.refbot.config.server.configureForGamePayloads
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.SystemUtils
import com.google.gson.GsonBuilder
import dev.kord.core.ClientResources
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class GameModeRequestTest {
    private val gson = GsonBuilder().configureForGamePayloads().create()
    private val discordMessageHandler = mockk<DiscordMessageHandler>()
    private val client = mockk<Kord>()
    private val gameThread = mockk<TextChannelThread>()
    private val gameModeRequest = GameModeRequest(discordMessageHandler, SystemUtils())

    private fun stubThreadLookupReturns(thread: TextChannelThread) {
        val resources = mockk<ClientResources>(relaxed = true)
        Kord::class.java.getDeclaredField("resources").apply { isAccessible = true }.set(client, resources)
        coEvery { client.getChannel(any(), any()) } returns thread
    }

    private fun game(
        gameMode: String,
        gameStatus: String,
    ): Game =
        gson.fromJson(
            """
            {
              "game_id": 1,
              "home_platform_id": "123456789",
              "game_status": "$gameStatus",
              "game_mode": "$gameMode"
            }
            """.trimIndent(),
            Game::class.java,
        )

    @Test
    fun `posts a chew mode enabled message to the game thread`() =
        runBlocking {
            stubThreadLookupReturns(gameThread)
            coEvery {
                discordMessageHandler.sendGameMessage(any(), any(), any(), any(), any(), any(), any())
            } returns mockk<Message>()

            gameModeRequest.notifyGameModeChange(client, game("CHEW", "OPENING_KICKOFF"))

            coVerify(exactly = 1) {
                discordMessageHandler.sendGameMessage(client, any(), Scenario.CHEW_MODE_ENABLED, null, null, gameThread, any())
            }
        }

    @Test
    fun `posts a chew mode disabled message when returning to normal`() =
        runBlocking {
            stubThreadLookupReturns(gameThread)
            coEvery {
                discordMessageHandler.sendGameMessage(any(), any(), any(), any(), any(), any(), any())
            } returns mockk<Message>()

            gameModeRequest.notifyGameModeChange(client, game("NORMAL", "OPENING_KICKOFF"))

            coVerify(exactly = 1) {
                discordMessageHandler.sendGameMessage(client, any(), Scenario.CHEW_MODE_DISABLED, null, null, gameThread, any())
            }
        }

    @Test
    fun `does not post when the game is already final`() =
        runBlocking {
            gameModeRequest.notifyGameModeChange(client, game("CHEW", "FINAL"))

            coVerify(exactly = 0) {
                discordMessageHandler.sendGameMessage(any(), any(), any(), any(), any(), any(), any())
            }
        }
}
