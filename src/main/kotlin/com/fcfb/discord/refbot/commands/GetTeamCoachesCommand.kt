package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class GetTeamCoachesCommand(
    private val teamClient: TeamClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "get_team_coaches",
            "Get the coaches for a team",
        ) {
            string("team", "Team") {
                required = true
            }
        }
    }

    /**
     * Get general game information
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is getting the team coaches in channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val command = interaction.command
        val teamName = command.options["team"]!!.value.toString()
        val apiResponse = teamClient.getTeamByName(teamName)
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val team = apiResponse.keys.firstOrNull()

        if (team != null) {
            val coachList = team.coachDiscordIds.map { interaction.kord.getUser(Snowflake(it)) }
            response.respond { this.content = "${team.name} Coaches: " + coachList.filterNotNull().joinToString(" ") { it.mention } }
            Logger.info(
                "${interaction.user.username} successfully grabbed team coaches in channel ${interaction.channelId.value}",
            )
        } else {
            response.respond { this.content = "Team is empty!" }
            Logger.error("${interaction.user.username} failed to get the team coaches in channel ${interaction.channelId.value}")
        }
    }
}
