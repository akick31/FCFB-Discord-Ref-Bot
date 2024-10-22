package com.fcfb.discord.refbot.config

import java.io.File
import java.util.Properties

object Config {

    /**
     * Load the configuration file
     * @return The configuration properties
     */
    fun loadConfig(): Properties {
        val configFile = File("resources/config.properties")
        return Properties().apply { load(configFile.inputStream()) }
    }
}
