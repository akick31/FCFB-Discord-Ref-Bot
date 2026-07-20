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

    /**
     * Creates a configured HTTP client with extended timeouts
     * for operations that may take longer (like play processing)
     */
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
                    connectTimeout = 30_000 // 30 seconds to connect
                    requestTimeout = 300_000 // 5 minutes for request completion
                    keepAliveTime = 60_000 // Keep connections alive for 1 minute
                }
            }

            // Install timeout plugin with explicit configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000 // 5 minutes
                connectTimeoutMillis = 30_000 // 30 seconds
                socketTimeoutMillis = 300_000 // 5 minutes
            }

            install(ContentNegotiation) {
                jackson {}
            }
        }
    }
}
