package com.fcfb.discord.refbot.api.utils

import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.dto.ApiResponse
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.statement.HttpResponse

class ApiUtils {
    fun errorFrom(
        response: HttpResponse,
        jsonResponse: String,
    ): String? {
        if (response.status.value in 200..299) {
            return null
        }
        val error =
            parseError(jsonResponse)
                ?: jsonResponse.ifBlank { "The API returned ${response.status.value} with an empty response" }
        Logger.warn("Backend Error: $error")
        return error
    }

    private fun parseError(jsonResponse: String): String? {
        if (jsonResponse.isBlank()) {
            return null
        }
        return try {
            JacksonConfig().configureApiResponseMapping()
                .readValue(jsonResponse, ApiResponse::class.java).error
        } catch (e: Exception) {
            null
        }
    }
}
