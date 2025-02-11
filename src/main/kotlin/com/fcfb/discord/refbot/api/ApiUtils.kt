package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.config.JacksonConfig
import com.fcfb.discord.refbot.model.response.ApiResponse
import com.fcfb.discord.refbot.utils.Logger

class ApiUtils {
    /**
     * Read the error from the JSON response
     * @param jsonResponse
     */
    fun readError(jsonResponse: String): String {
        val objectMapper = JacksonConfig().configureApiResponseMapping()
        val error = objectMapper.readValue(jsonResponse, ApiResponse::class.java)
        Logger.error("Backend Error: ${error.error}")
        return error.error
    }
}