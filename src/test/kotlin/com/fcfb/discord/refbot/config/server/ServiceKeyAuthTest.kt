package com.fcfb.discord.refbot.config.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceKeyAuthTest {
    private val expected = "s3cr3t-bot-service-key"

    @Test
    fun acceptsTheMatchingKey() {
        assertTrue(isValidServiceKey(expected, expected))
    }

    @Test
    fun rejectsMissingKey() {
        assertFalse(isValidServiceKey(null, expected))
        assertFalse(isValidServiceKey("", expected))
    }

    @Test
    fun rejectsWrongKey() {
        assertFalse(isValidServiceKey("not-the-key", expected))
        assertFalse(isValidServiceKey("${expected}x", expected))
        assertFalse(isValidServiceKey(expected.dropLast(1), expected))
    }
}
