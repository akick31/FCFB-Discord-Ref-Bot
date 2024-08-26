package utils

import org.slf4j.LoggerFactory

object Logger {
    private val logger = LoggerFactory.getLogger(Logger::class.java)

    fun debug(message: String, vararg args: Any?) {
        logger.debug(message, *args)
    }

    fun info(message: String, vararg args: Any?) {
        logger.info(message, *args)
    }

    fun warn(message: String, vararg args: Any?) {
        logger.warn(message, *args)
    }

    fun error(message: String, vararg args: Any?) {
        logger.error(message, *args)
    }
}
