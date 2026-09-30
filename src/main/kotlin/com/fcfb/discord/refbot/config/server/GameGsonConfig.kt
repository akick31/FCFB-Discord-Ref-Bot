package com.fcfb.discord.refbot.config.server

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.text.DateFormat

fun GsonBuilder.configureForGamePayloads(): GsonBuilder =
    setDateFormat(DateFormat.LONG)
        .setPrettyPrinting()
        .serializeNulls()
        .disableHtmlEscaping()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
