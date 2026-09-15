package com.fcfb.discord.refbot.utils.system

import java.util.concurrent.atomic.AtomicBoolean

class DiscordReadinessState {
    private val ready = AtomicBoolean(false)

    fun markReady() = ready.set(true)

    fun markNotReady() = ready.set(false)

    fun isReady() = ready.get()
}
