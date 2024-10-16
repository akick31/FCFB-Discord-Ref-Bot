package com.fcfb.discord_ref_bot.config

import java.io.File
import java.util.Properties

object Config {
    fun loadConfig(): Properties {
        val configFile = File("resources/config.properties")
        return Properties().apply { load(configFile.inputStream()) }
    }
}
