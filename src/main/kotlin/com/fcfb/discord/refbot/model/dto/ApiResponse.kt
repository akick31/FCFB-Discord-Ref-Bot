package com.fcfb.discord.refbot.model.dto

data class ApiResponse(
    val error: String,
    val timestamp: String? = null,
    val status: String? = null,
    val path: String? = null,
)
