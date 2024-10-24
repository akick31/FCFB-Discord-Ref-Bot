package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.rest.builder.interaction.string

class StartGameCommand {
    private val gameClient = GameClient()

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "start_game",
            "Start a new game",
        ) {
            string("season", "Season") {
                required = true
            }
            string("week", "Week") {
                required = true
            }
            string("subdivision", "Subdivision") {
                required = true
                mutableListOf(
                    choice("FCFB", "FCFB"),
                    choice("FBS", "FBS"),
                    choice("FCS", "FCS"),
                )
            }
            string("home_team", "Home Team") {
                required = true
            }
            string("away_team", "Away Team") {
                required = true
            }
            string("tv_channel", "TV Channel") {
                required = true
                mutableListOf(
                    choice("ABC", "ABC"),
                    choice("CBS", "CBS"),
                    choice("ESPN", "ESPN"),
                    choice("ESPN2", "ESPN2"),
                    choice("FOX", "FOX"),
                    choice("FS1", "FS1"),
                    choice("FS2", "FS2"),
                    choice("NBC", "NBC"),
                )
            }
            string("start_time", "Start Time") {
                required = true
                mutableListOf(
                    choice("12:00 PM EST", "12:00 PM EST"),
                    choice("3:30 PM EST", "3:30 PM EST"),
                    choice("7:00 PM EST", "7:00 PM EST"),
                    choice("10:30 PM EST", "10:30 PM EST"),
                )
            }
            string("location", "Location") {
                required = true
            }
            string("game_type", "Game Type") {
                required = true
                mutableListOf(
                    choice("Out of Conference", "Out of Conference"),
                    choice("Conference Game", "Conference Game"),
                    choice("Conference Championship", "Conference Championship"),
                    choice("Bowl Game", "Bowl Game"),
                    choice("Playoff Game", "Playoff Game"),
                    choice("National Championship", "National Championship"),
                    choice("Scrimmage", "Scrimmage"),
                )
            }
        }
    }

    /**
     * Start a new game
     */
    suspend fun execute(
        userRole: Role,
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand,
    ) {
        Logger.info(
            "${interaction.user.username} is starting a game between ${command.options["home_team"]!!.value}" +
                " and ${command.options["away_team"]!!.value}",
        )
        val response = interaction.deferPublicResponse()

        if (userRole == Role.USER) {
            response.respond { this.content = "You do not have permission to start a game" }
            Logger.error("${interaction.user.username} does not have permission to start a game")
            return
        }

        val season = command.options["season"]!!.value.toString()
        val week = command.options["week"]!!.value.toString()
        val subdivisionString = command.options["subdivision"]!!.value.toString()
        val subdivision =
            when (subdivisionString) {
                "FCFB" -> {
                    Subdivision.FCFB
                }
                "FBS" -> {
                    Subdivision.FBS
                }
                "FCS" -> {
                    Subdivision.FCS
                }
                else -> {
                    Subdivision.FCFB
                }
            }
        val homeTeam = command.options["home_team"]!!.value.toString()
        val awayTeam = command.options["away_team"]!!.value.toString()
        val tvChannelString = command.options["tv_channel"]!!.value.toString()
        val tvChannel =
            when (tvChannelString) {
                "ABC" -> {
                    TVChannel.ABC
                }
                "CBS" -> {
                    TVChannel.CBS
                }
                "ESPN" -> {
                    TVChannel.ESPN
                }
                "ESPN2" -> {
                    TVChannel.ESPN2
                }
                "FOX" -> {
                    TVChannel.FOX
                }
                "FS1" -> {
                    TVChannel.FS1
                }
                "FS2" -> {
                    TVChannel.FS2
                }
                "NBC" -> {
                    TVChannel.NBC
                }
                else -> null
            }
        val startTime = command.options["start_time"]!!.value.toString()
        val location = command.options["location"]!!.value.toString()
        val gameTypeString = command.options["game_type"]!!.value.toString()
        val gameType =
            when (gameTypeString) {
                "Out of Conference" -> {
                    GameType.OUT_OF_CONFERENCE
                }
                "Conference Game" -> {
                    GameType.CONFERENCE_GAME
                }
                "Conference Championship" -> {
                    GameType.CONFERENCE_CHAMPIONSHIP
                }
                "Bowl Game" -> {
                    GameType.BOWL
                }
                "Playoff Game" -> {
                    GameType.PLAYOFFS
                }
                "National Championship" -> {
                    GameType.NATIONAL_CHAMPIONSHIP
                }
                "Scrimmage" -> {
                    GameType.SCRIMMAGE
                }
                else -> GameType.SCRIMMAGE
            }

        val startedGame = gameClient.startGame(season, week, subdivision, homeTeam, awayTeam, tvChannel, startTime, location, gameType)
        if (startedGame == null) {
            response.respond { this.content = "Start game failed!" }
            Logger.error("${interaction.user.username} failed to start a game between $homeTeam and $awayTeam")
        } else {
            response.respond { this.content = "Started game between $homeTeam and $awayTeam" }
            Logger.info("${interaction.user.username} successfully started a game between $homeTeam and $awayTeam")
        }
        // TODO add option to check db if game WAS created and remove it
    }
}
