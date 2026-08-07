package com.fcfb.discord.refbot.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson

object HttpClientConfig {
    private fun loadServiceKey(): String {
        val stream =
            this::class.java.classLoader.getResourceAsStream("application.properties")
                ?: throw RuntimeException("application.properties file not found")
        val properties = java.util.Properties()
        properties.load(stream)
        return properties.getProperty("bot.service.key")
            ?: throw RuntimeException("bot.service.key not set in application.properties")
    }

    fun createClient(): HttpClient {
        val serviceKey = loadServiceKey()
        return HttpClient(CIO) {
            defaultRequest {
                header("X-Service-Key", serviceKey)
            }
            engine {
                maxConnectionsCount = 64
                endpoint {
                    maxConnectionsPerRoute = 8
                    connectTimeout = 30_000
                    requestTimeout = 300_000
                    keepAliveTime = 60_000
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 300_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 300_000
            }

            install(ContentNegotiation) {
                jackson {}
            }
        }
    }
}
