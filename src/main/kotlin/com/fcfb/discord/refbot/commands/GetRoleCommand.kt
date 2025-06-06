package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class GetRoleCommand(
    private val userClient: UserClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "get_role",
            "Get user role",
        )
    }

    /**
     * Get general role information
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is getting user role in channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()
        val apiResponse = userClient.getUserByDiscordId(interaction.user.id.value.toString())
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val user = apiResponse.keys.firstOrNull()

        if (user != null) {
            val messageContent = "User role: ${user.role}"
            response.respond { this.content = messageContent }
            Logger.info(
                "${interaction.user.username} successfully grabbed user role for a user in channel ${interaction.channelId.value}",
            )
        } else {
            response.respond { this.content = "User role command failed!" }
            Logger.error("${interaction.user.username} failed to get user role for a user in channel ${interaction.channelId.value}")
        }
    }
}
