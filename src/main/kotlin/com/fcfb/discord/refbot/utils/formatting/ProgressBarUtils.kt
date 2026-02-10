package com.fcfb.discord.refbot.utils.formatting

object ProgressBarUtils {
    /**
     * Build a progress bar string for displaying job progress.
     * @param current The current progress index
     * @param total The total number of items
     * @return A formatted progress bar string (e.g., "[####----] 50%")
     */
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
