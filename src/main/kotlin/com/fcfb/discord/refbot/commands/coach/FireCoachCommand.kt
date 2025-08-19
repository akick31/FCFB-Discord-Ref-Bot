package com.fcfb.discord.refbot.commands.coach

import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class FireCoachCommand(
    private val teamClient: TeamClient,
) {
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

        val apiResponse = teamClient.fireCoach(team, interaction.user.username)
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val updatedTeam = apiResponse.keys.firstOrNull()
        if (updatedTeam == null) {
            response.respond { this.content = "Team fire failed!" }
            Logger.error("${interaction.user.username} failed to fire a coach for ${command.options["team"]!!.value}")
        } else {
            response.respond { this.content = "Fired all of the coaches for $team" }
            Logger.info("${interaction.user.username} successfully fired a coach for ${command.options["team"]!!.value}")
        }
    }
}
