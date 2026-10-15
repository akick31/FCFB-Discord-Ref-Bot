package com.fcfb.discord.refbot.config.server

import com.fcfb.discord.refbot.handlers.api.GameFinalizationRollbackRequest
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.utils.system.DiscordReadinessState
import com.fcfb.discord.refbot.utils.system.Properties
import dev.kord.core.Kord
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameFinalizationRollbackRouteTest {
    private val client = mockk<Kord>()
    private val messages = mockk<DiscordMessageHandler>()
    private val readiness = mockk<DiscordReadinessState>()
    private val config =
        KtorServerConfig(messages, mockk(), mockk(), mockk(), mockk(), readiness, GameFinalizationRollbackRequest(messages))
    private val server = embeddedServer(Netty, port = 0) { config.run { configureServer(client, null, null) } }
    private lateinit var url: URI
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    @BeforeTest
    fun startServer() {
        mockkConstructor(Properties::class)
        every { anyConstructed<Properties>().getServiceKey() } returns "test-key"
        every { readiness.isReady() } returns true
        coEvery { messages.sendNotificationToCommissioners(any(), any()) } returns mockk()
        server.start(wait = false)
        val port = runBlocking { server.resolvedConnectors().single().port }
        url = URI("http://127.0.0.1:$port/fcfb_discord/game_finalization_rollback")
    }

    @AfterTest
    fun stopServer() {
        server.stop(0, 1000)
        unmockkConstructor(Properties::class)
    }

    private fun post(
        body: String,
        key: String? = "test-key",
    ): HttpResponse<String> {
        val request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
        if (key != null) request.header("X-Service-Key", key)
        return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
    }

    private val payload = """{"game_id":2211,"home_team":"Ohio State","away_team":"Michigan"}"""

    @Test
    fun `snake-case contract posts through the existing commissioner notification channel`() {
        assertEquals(200, post(payload).statusCode())
        coVerify(exactly = 1) {
            messages.sendNotificationToCommissioners(
                client,
                "Game 2211: Ohio State vs Michigan. The game could not save its final results. " +
                    "Changes made while saving those results were undone. " +
                    "Ask an administrator to check the game before trying to finish it again.",
            )
        }
    }

    @Test
    fun `missing or wrong service key cannot post`() {
        assertEquals(401, post(payload, null).statusCode())
        assertEquals(401, post(payload, "wrong").statusCode())
        coVerify(exactly = 0) { messages.sendNotificationToCommissioners(any(), any()) }
    }

    @Test
    fun `missing invalid and malformed payloads cannot post`() {
        for (body in listOf(
            "{}",
            "null",
            "{",
            """{"game_id":0,"home_team":"Home","away_team":"Away"}""",
            """{"game_id":1,"home_team":"","away_team":"Away"}""",
        )) {
            assertEquals(400, post(body).statusCode(), body)
        }
        coVerify(exactly = 0) { messages.sendNotificationToCommissioners(any(), any()) }
    }

    @Test
    fun `Discord delivery failure returns a sanitized error`() {
        coEvery { messages.sendNotificationToCommissioners(any(), any()) } throws IllegalStateException("private token or path")
        val response = post(payload)
        assertEquals(500, response.statusCode())
        assertEquals("Unable to report finalization rollback", response.body())
    }
}
