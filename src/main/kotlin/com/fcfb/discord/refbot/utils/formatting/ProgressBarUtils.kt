package com.fcfb.discord.refbot.utils.formatting

object ProgressBarUtils {
    fun buildProgressBar(
        current: Int,
        total: Int,
    ): String {
        if (total == 0) return "`[--------------------] 0%`"
        val percent = (current.toDouble() / total * 100).toInt().coerceIn(0, 100)
        val filled = (percent / 5).coerceIn(0, 20)
        val empty = 20 - filled
        return "`[${"#".repeat(filled)}${"-".repeat(empty)}] $percent%`"
    }
}
