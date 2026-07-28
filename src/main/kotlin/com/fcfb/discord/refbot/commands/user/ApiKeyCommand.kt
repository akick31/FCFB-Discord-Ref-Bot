package com.fcfb.discord.refbot.commands.user

import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class ApiKeyCommand(
    private val fcfbUserClient: FCFBUserClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "api_key",
            "Generate a personal FCFB API key (only you can see the response)",
        )
    }

    /**
     * Generate a personal API key and return it to the invoking user in an ephemeral (private) response.
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferEphemeralResponse()
        val discordId = interaction.user.id.value.toString()

        val userResult = fcfbUserClient.getUserByDiscordId(discordId)
        val user = userResult.keys.firstOrNull()
        val userId = user?.id
        if (userId == null) {
            response.respond {
                content = userResult.values.firstOrNull() ?: "Could not find your FCFB account. Are you registered?"
            }
            return
        }

        val keyResult = fcfbUserClient.generateApiKey(userId)
        val apiKey = keyResult.keys.firstOrNull()
        if (apiKey == null) {
            response.respond {
                content = "Failed to generate an API key: ${keyResult.values.firstOrNull() ?: "unknown error"}"
            }
            Logger.error("${interaction.user.username} failed to generate an API key")
            return
        }

        response.respond {
            content =
                buildString {
                    appendLine("Here is your personal FCFB API key. **Only you can see this message.**")
                    appendLine("```$apiKey```")
                    appendLine("Send it as the `X-Api-Key` header on your API requests.")
                    appendLine(
                        "Keep it secret — anyone with this key can act as you. " +
                            "Run `/api_key` again to rotate it, which invalidates the old key.",
                    )
                }
        }
        Logger.info("${interaction.user.username} generated a personal API key")
    }
}
