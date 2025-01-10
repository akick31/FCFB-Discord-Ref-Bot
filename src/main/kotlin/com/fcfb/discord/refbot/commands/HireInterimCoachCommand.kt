package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

class HireInterimCoachCommand(
    private val teamClient: TeamClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "hire_interim_coach",
            "Hire an interim coach for a team",
        ) {
            user("coach", "Coach") {
                required = true
            }
            string("team", "Team") {
                required = true
            }
        }
    }

    /**
     * Hire a new coach for a team
     * @param interaction The interaction object
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        Logger.info("${interaction.user.username} is hiring an interim coach for ${command.options["team"]!!.value}")
        val response = interaction.deferPublicResponse()

        val coach = command.users["coach"]!!
        val team = command.options["team"]!!.value.toString()

        val updatedTeam = teamClient.hireInterimCoach(team, coach.id.value.toString(), interaction.user.username)
        if (updatedTeam == null) {
            response.respond { this.content = "Team interim hire failed!" }
            Logger.error("${interaction.user.username} failed to hire a new coach for ${command.options["team"]!!.value}")
        } else {
            response.respond { this.content = "Hired ${coach.username} as interim for $team" }
            Logger.info("${interaction.user.username} successfully hired a new interim coach for ${command.options["team"]!!.value}")
        }
    }
}
