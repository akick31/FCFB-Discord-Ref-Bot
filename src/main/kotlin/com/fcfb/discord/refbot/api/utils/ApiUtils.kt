package com.fcfb.discord.refbot.api.utils

import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.dto.ApiResponse
import com.fcfb.discord.refbot.utils.system.Logger

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
