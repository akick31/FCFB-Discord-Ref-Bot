package com.fcfb.discord.refbot.model.infrastructure

data class Health(
    val status: String,
    val jobs: Map<String, Boolean>?,
    val memory: Map<String, String>?,
    val diskSpace: Map<String, String>?,
    val kord: Map<String, String>?,
    val message: String? = null,
)
