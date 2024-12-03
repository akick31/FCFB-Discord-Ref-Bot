package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

class FireCoachCommand {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "fire_coach",
            "Fire a coach for a team",
        ) {
            string("team", "Team") {
                required = true
            }
        }
    }

    /**
     * Hire a new coach for a team
     * @param interaction The interaction object
     * @param command The command object
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        Logger.info("${interaction.user.username} is fire the coach for ${command.options["team"]!!.value}")
        val response = interaction.deferPublicResponse()

        val team = command.options["team"]!!.value.toString()

        val updatedTeam = TeamClient().fireCoach(team)
        if (updatedTeam == null) {
            response.respond { this.content = "Team fire failed!" }
            Logger.error("${interaction.user.username} failed to fire a coach for ${command.options["team"]!!.value}")
        } else {
            response.respond { this.content = "Fired all of the coaches for $team" }
            Logger.info("${interaction.user.username} successfully fired a coach for ${command.options["team"]!!.value}")
        }
    }
}
