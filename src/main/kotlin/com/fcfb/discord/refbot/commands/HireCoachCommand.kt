package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.model.fcfb.CoachPosition
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

class HireCoachCommand {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "hire_coach",
            "Hire a coach for a team",
        ) {
            user("coach", "Coach") {
                required = true
            }
            string("team", "Team") {
                required = true
            }
            string("position", "Position Hiring For") {
                required = true
                mutableListOf(
                    choice("Head Coach", "Head Coach"),
                    choice("Offensive Coordinator", "Offensive Coordinator"),
                    choice("Defensive Coordinator", "Defensive Coordinator"),
                )
            }
        }
    }

    /**
     * Hire a new coach for a team
     * @param userRole The role of the user
     * @param interaction The interaction object
     * @param command The command object
     */
    suspend fun execute(
        userRole: Role,
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand,
    ) {
        Logger.info("${interaction.user.username} is hiring a new coach for ${command.options["team"]!!.value}")
        val response = interaction.deferPublicResponse()

        if (userRole == Role.USER) {
            response.respond { this.content = "You do not have permission to hire a coach" }
            Logger.error("${interaction.user.username} does not have permission to hire a coach")
            return
        }

        val coach = command.users["coach"]!!
        val team = command.options["team"]!!.value.toString()
        val positionString = command.options["position"]!!.value.toString()

        val position =
            when (positionString) {
                "Head Coach" -> {
                    CoachPosition.HEAD_COACH
                }
                "Offensive Coordinator" -> {
                    CoachPosition.OFFENSIVE_COORDINATOR
                }
                else -> {
                    CoachPosition.DEFENSIVE_COORDINATOR
                }
            }

        val updatedTeam = TeamClient().hireCoach(team, coach.id.value.toString(), position)
        if (updatedTeam == null) {
            response.respond { this.content = "Team hire failed!" }
            Logger.error("${interaction.user.username} failed to hire a new coach for ${command.options["team"]!!.value}")
        } else {
            response.respond { this.content = "Hired ${coach.username} for $team" }
            Logger.info("${interaction.user.username} successfully hired a new coach for ${command.options["team"]!!.value}")
        }
    }
}
