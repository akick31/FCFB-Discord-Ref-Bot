package com.fcfb.discord.refbot.commands.system

import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.model.domain.FCFBUser
import com.fcfb.discord.refbot.model.domain.Team
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class DogReportCommand(
    private val fcfbUserClient: FCFBUserClient,
    private val teamClient: TeamClient,
) {
    companion object {
        const val COMMAND_NAME = "generate_dog_report"
        const val COMMAND_DESCRIPTION = "Generate a Delay of Game report grouped by team"
    }

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            COMMAND_NAME,
            COMMAND_DESCRIPTION,
        )
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info("${interaction.user.username} requested a Delay of Game report")
        val response = interaction.deferPublicResponse()

        try {
            val usersResult = fcfbUserClient.getAllUsers()
            val users: List<FCFBUser>? = usersResult.keys.firstOrNull()
            val usersError = usersResult.values.firstOrNull()

            if (users == null) {
                response.respond {
                    content = usersError ?: "Failed to load users for Delay of Game report."
                }
                return
            }

            val teamsResult = teamClient.getAllTeams()
            val teams: List<Team>? = teamsResult.keys.firstOrNull()
            val teamsError = teamsResult.values.firstOrNull()

            if (teams == null) {
                response.respond {
                    content = teamsError ?: "Failed to load teams for Delay of Game report."
                }
                return
            }

            val activeTeams = teams.filter { it.active }

            // Aggregate delay-of-game instances by team name
            val dogByTeam =
                users
                    .filter { !it.team.isNullOrBlank() && it.delayOfGameInstances > 0 }
                    .groupBy { it.team!! }
                    .mapValues { (_, teamUsers) -> teamUsers.sumOf { user -> user.delayOfGameInstances } }

            if (dogByTeam.isEmpty()) {
                response.respond {
                    content = "No Delay of Game instances have been recorded for any team this season."
                }
                return
            }

            val rows =
                activeTeams
                    .mapNotNull { team ->
                        val name = team.name ?: return@mapNotNull null
                        val count = dogByTeam[name] ?: 0
                        if (count > 0) {
                            name to count
                        } else {
                            null
                        }
                    }
                    .sortedWith(
                        compareByDescending<Pair<String, Int>> { it.second }
                            .thenBy { it.first },
                    )

            if (rows.isEmpty()) {
                response.respond {
                    content = "No active teams currently have any Delay of Game instances."
                }
                return
            }

            val header = String.format("%-26s %4s", "Team", "DOGs")
            val separator = "-".repeat(header.length)
            val bodyLines = rows.map { (teamName, count) -> String.format("%-26s %4d", teamName, count) }

            val reportText =
                buildString {
                    append("**Delay of Game Report (by Team)**\n")
                    append("```text\n")
                    append(header).append("\n")
                    append(separator).append("\n")
                    bodyLines.forEach { line ->
                        append(line).append("\n")
                    }
                    append("```")
                }

            response.respond {
                content = reportText
            }
        } catch (e: Exception) {
            Logger.error("Failed to generate Delay of Game report: ${e.message}", e)
            response.respond {
                content = "An unexpected error occurred while generating the Delay of Game report."
            }
        }
    }
}

