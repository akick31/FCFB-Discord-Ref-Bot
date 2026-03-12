package com.fcfb.discord.refbot.commands.system

import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
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
    private val playClient: PlayClient,
) {
    companion object {
        const val COMMAND_NAME = "generate_dog_report"
        const val COMMAND_DESCRIPTION = "Generate a Delay of Game report grouped by team for a given week"

        const val SEASON_OPTION = "season"
        const val SEASON_DESCRIPTION = "Season number"

        const val WEEK_OPTION = "week"
        const val WEEK_DESCRIPTION = "Week number"
    }

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            COMMAND_NAME,
            COMMAND_DESCRIPTION,
        ) {
            integer(SEASON_OPTION, SEASON_DESCRIPTION) {
                required = true
            }
            integer(WEEK_OPTION, WEEK_DESCRIPTION) {
                required = true
            }
        }
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val season = interaction.command.integers[SEASON_OPTION]!!.toInt()
        val week = interaction.command.integers[WEEK_OPTION]!!.toInt()

        Logger.info("${interaction.user.username} requested a Delay of Game report for season $season week $week")
        val response = interaction.deferPublicResponse()

        try {
            val result = playClient.getDelayOfGameCountsByWeek(season, week)
            val dogByTeam = result.keys.firstOrNull()
            val error = result.values.firstOrNull()

            if (dogByTeam == null) {
                response.respond {
                    content = error ?: "Failed to load Delay of Game data."
                }
                return
            }

            if (dogByTeam.isEmpty()) {
                response.respond {
                    content = "No Delay of Game instances recorded for season $season, week $week."
                }
                return
            }

            val rows =
                dogByTeam.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .map { it.key to it.value }

            val imageBytes = generateReportImage(rows, season, week)
            val filePath = saveImageToFile(imageBytes, season, week)

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

    private fun generateReportImage(rows: List<Pair<String, Int>>, season: Int, week: Int): ByteArray {
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
        g.drawString("Season $season — Week $week", paddingH, paddingV + 48)

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

            // DOG count — red if 3 (max per week), otherwise normal
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

    private fun saveImageToFile(imageBytes: ByteArray, season: Int, week: Int): String {
        val imagesDir = File("images")
        if (!imagesDir.exists()) {
            if (imagesDir.mkdirs()) {
                Logger.info("Created images directory: ${imagesDir.absolutePath}")
            } else {
                Logger.error("Failed to create images directory.")
            }
        }
        val fileName = "images/dog_report_s${season}_w${week}_${System.currentTimeMillis()}.png"
        Files.write(Paths.get(fileName), imageBytes, StandardOpenOption.CREATE)
        return fileName
    }
}
