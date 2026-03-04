package com.fcfb.discord.refbot.commands.system

import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.message.addFile
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

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
            val users = usersResult.keys.firstOrNull()
            val usersError = usersResult.values.firstOrNull()

            if (users == null) {
                response.respond {
                    content = usersError ?: "Failed to load users for Delay of Game report."
                }
                return
            }

            val teamsResult = teamClient.getAllTeams()
            val teams = teamsResult.keys.firstOrNull()
            val teamsError = teamsResult.values.firstOrNull()

            if (teams == null) {
                response.respond {
                    content = teamsError ?: "Failed to load teams for Delay of Game report."
                }
                return
            }

            val activeTeams = teams.filter { it.active }

            // Aggregate delay-of-game instances by team name (only users with > 0)
            val dogByTeam =
                users
                    .filter { !it.team.isNullOrBlank() && it.delayOfGameInstances > 0 }
                    .groupBy { it.team!! }
                    .mapValues { (_, teamUsers) -> teamUsers.sumOf { it.delayOfGameInstances } }

            if (dogByTeam.isEmpty()) {
                response.respond {
                    content = "No Delay of Game instances have been recorded for any team this season."
                }
                return
            }

            // Only include active teams that have at least one DOG
            val rows =
                activeTeams
                    .mapNotNull { team ->
                        val name = team.name ?: return@mapNotNull null
                        val count = dogByTeam[name] ?: return@mapNotNull null
                        name to count
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

            val imageBytes = generateReportImage(rows)
            val filePath = saveImageToFile(imageBytes)

            response.respond {
                addFile(Paths.get(filePath))
            }
        } catch (e: Exception) {
            Logger.error("Failed to generate Delay of Game report: ${e.message}", e)
            response.respond {
                content = "An unexpected error occurred while generating the Delay of Game report."
            }
        }
    }

    private fun generateReportImage(rows: List<Pair<String, Int>>): ByteArray {
        val paddingH = 24
        val paddingV = 20
        val rowHeight = 32
        val titleHeight = 58
        val headerHeight = 36
        val width = 420
        val height = paddingV + titleHeight + headerHeight + rows.size * rowHeight + paddingV

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Background
        g.color = Color(30, 31, 34)
        g.fillRect(0, 0, width, height)

        // Title
        g.color = Color(255, 255, 255)
        g.font = Font("SansSerif", Font.BOLD, 20)
        g.drawString("Delay of Game Report", paddingH, paddingV + 28)

        // Subtitle
        g.color = Color(148, 155, 164)
        g.font = Font("SansSerif", Font.PLAIN, 13)
        g.drawString("Active teams with DOG infractions this season", paddingH, paddingV + 48)

        // Header background
        val headerY = paddingV + titleHeight
        g.color = Color(47, 49, 54)
        g.fillRect(0, headerY, width, headerHeight)

        // Header text
        g.color = Color(185, 187, 190)
        g.font = Font("SansSerif", Font.BOLD, 13)
        val dogsLabel = "DOGs"
        g.drawString("TEAM", paddingH, headerY + 24)
        g.drawString(dogsLabel, width - paddingH - g.fontMetrics.stringWidth(dogsLabel), headerY + 24)

        // Header bottom divider
        g.color = Color(64, 68, 75)
        g.drawLine(0, headerY + headerHeight - 1, width, headerY + headerHeight - 1)

        // Rows
        rows.forEachIndexed { i, (team, count) ->
            val rowY = headerY + headerHeight + i * rowHeight

            g.color = if (i % 2 == 0) Color(37, 38, 40) else Color(32, 34, 37)
            g.fillRect(0, rowY, width, rowHeight)

            g.color = Color(220, 221, 222)
            g.font = Font("SansSerif", Font.PLAIN, 13)
            g.drawString(team, paddingH, rowY + 21)

            // DOG count — red if 3+, otherwise normal
            val countStr = count.toString()
            g.color = if (count >= 3) Color(240, 71, 71) else Color(220, 221, 222)
            g.font = Font("SansSerif", Font.BOLD, 13)
            g.drawString(countStr, width - paddingH - g.fontMetrics.stringWidth(countStr), rowY + 21)
        }

        // Bottom border
        g.color = Color(64, 68, 75)
        g.drawLine(0, height - 1, width, height - 1)

        g.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    private fun saveImageToFile(imageBytes: ByteArray): String {
        val imagesDir = File("images")
        if (!imagesDir.exists()) {
            if (imagesDir.mkdirs()) {
                Logger.info("Created images directory: ${imagesDir.absolutePath}")
            } else {
                Logger.error("Failed to create images directory.")
            }
        }
        val fileName = "images/dog_report_${System.currentTimeMillis()}.png"
        Files.write(Paths.get(fileName), imageBytes, StandardOpenOption.CREATE)
        return fileName
    }
}
