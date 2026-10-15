package com.fcfb.discord.refbot.utils.polling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameWeekPollingUtilsTest {
    @Test
    fun `interrupted job does not claim all games started`() {
        val result = GameWeekPollingUtils.PollingResult(true, "FAILED", 3, 1, 0, 1)
        assertEquals(
            "Processing stopped with 2 game(s) unfinished. " +
                "Check the schedule and existing game threads before restarting remaining games.",
            GameWeekPollingUtils.interruptedJobMessage(result),
        )
    }

    @Test
    fun `interrupted job distinguishes unfinished games from recorded failures`() {
        val result = GameWeekPollingUtils.PollingResult(true, "FAILED", 4, 1, 1, 2)
        val message = GameWeekPollingUtils.interruptedJobMessage(result)!!
        assertTrue(message.contains("2 game(s) unfinished"))
        assertTrue(message.contains("`/retry_week` for the 1 recorded failure(s)"))
    }

    @Test
    fun `fully processed success and failure retain existing completion messages`() {
        assertNull(GameWeekPollingUtils.interruptedJobMessage(GameWeekPollingUtils.PollingResult(true, "COMPLETED", 2, 2, 0, 2)))
        assertNull(GameWeekPollingUtils.interruptedJobMessage(GameWeekPollingUtils.PollingResult(true, "FAILED", 2, 1, 1, 2)))
    }
}
