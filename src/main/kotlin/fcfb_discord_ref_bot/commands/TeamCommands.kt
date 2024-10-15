package fcfb_discord_ref_bot.commands

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import fcfb_discord_ref_bot.api.AuthClient
import fcfb_discord_ref_bot.api.TeamClient
import fcfb_discord_ref_bot.model.fcfb.CoachPosition
import fcfb_discord_ref_bot.model.fcfb.Role
import utils.Logger

class TeamCommands {
    suspend fun hireCoach(
        userRole: Role,
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand
    ) {
        Logger.info("${interaction.user.username} is hiring a new coach for ${command.options["team"]!!.value.toString()}")
        val response = interaction.deferPublicResponse()

        if (userRole == Role.USER) {
            response.respond { this.content = "You do not have permission to hire a coach" }
            Logger.error("${interaction.user.username} does not have permission to hire a coach")
            return
        }

        val coach = command.users["coach"]!!
        val team = command.options["team"]!!.value.toString()
        val positionString = command.options["position"]!!.value.toString()

        val position = when (positionString) {
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
            Logger.error("${interaction.user.username} failed to hire a new coach for ${command.options["team"]!!.value.toString()}")
        } else {
            response.respond { this.content = "Hired ${coach.username} for $team" }
            Logger.info("${interaction.user.username} successfully hired a new coach for for ${command.options["team"]!!.value.toString()}")
        }
    }
}