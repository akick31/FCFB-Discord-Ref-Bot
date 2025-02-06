package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.AuthClient
import com.fcfb.discord.refbot.model.fcfb.CoachPosition
import com.fcfb.discord.refbot.model.fcfb.FCFBUser
import com.fcfb.discord.refbot.model.fcfb.game.DefensivePlaybook
import com.fcfb.discord.refbot.model.fcfb.game.OffensivePlaybook
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class RegisterCommand(
    private val authClient: AuthClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "register",
            "Register a user to FCFB",
        ) {
            string("username", "Username") {
                required = true
            }
            string("coach_name", "Coach Name") {
                required = true
            }
            string("email", "Email") {
                required = true
            }
            string("password", "Password") {
                required = true
            }
            string("position", "Position") {
                required = true
                mutableListOf(
                    choice("Head Coach", "Head Coach"),
                    choice("Offensive Coordinator", "Offensive Coordinator"),
                    choice("Defensive Coordinator", "Defensive Coordinator"),
                )
            }
            string("offensive_playbook", "Offensive Playbook") {
                required = true
                mutableListOf(
                    choice("Air Raid", OffensivePlaybook.AIR_RAID.toString()),
                    choice("Spread", OffensivePlaybook.SPREAD.toString()),
                    choice("Pro", OffensivePlaybook.PRO.toString()),
                    choice("Flexbone", OffensivePlaybook.FLEXBONE.toString()),
                    choice("West Coast", OffensivePlaybook.WEST_COAST.toString()),
                )
            }
            string("defensive_playbook", "Defensive Playbook") {
                required = true
                mutableListOf(
                    choice("4-3", DefensivePlaybook.FOUR_THREE.toString()),
                    choice("3-4", DefensivePlaybook.THREE_FOUR.toString()),
                    choice("5-2", DefensivePlaybook.FIVE_TWO.toString()),
                    choice("4-4", DefensivePlaybook.FOUR_FOUR.toString()),
                    choice("3-3-5", DefensivePlaybook.THREE_THREE_FIVE.toString()),
                )
            }
        }
    }

    /**
     * Register a new user
     * @param interaction The interaction object
     * @param command The command object
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info("${interaction.user.username} is registering a new user")
        val command = interaction.command
        val response = interaction.deferEphemeralResponse()
        val username = command.options["username"]!!.value.toString()
        val coachName = command.options["coach_name"]!!.value.toString()
        val discordUsername = interaction.user.username
        val email = command.options["email"]!!.value.toString()
        val password = command.options["password"]!!.value.toString()
        val positionString = command.options["position"]!!.value.toString()
        val offensivePlaybookString = command.options["offensive_playbook"]!!.value.toString()
        val defensivePlaybookString = command.options["defensive_playbook"]!!.value.toString()

        val offensivePlaybook =
            if (offensivePlaybookString == OffensivePlaybook.AIR_RAID.toString()) {
                OffensivePlaybook.AIR_RAID
            } else if (offensivePlaybookString == OffensivePlaybook.SPREAD.toString()) {
                OffensivePlaybook.SPREAD
            } else if (offensivePlaybookString == OffensivePlaybook.PRO.toString()) {
                OffensivePlaybook.PRO
            } else if (offensivePlaybookString == OffensivePlaybook.FLEXBONE.toString()) {
                OffensivePlaybook.FLEXBONE
            } else {
                OffensivePlaybook.WEST_COAST
            }
        val defensivePlaybook =
            when (defensivePlaybookString) {
                DefensivePlaybook.FOUR_THREE.toString() -> {
                    DefensivePlaybook.FOUR_THREE
                }
                DefensivePlaybook.THREE_FOUR.toString() -> {
                    DefensivePlaybook.THREE_FOUR
                }
                DefensivePlaybook.FIVE_TWO.toString() -> {
                    DefensivePlaybook.FIVE_TWO
                }
                DefensivePlaybook.FOUR_FOUR.toString() -> {
                    DefensivePlaybook.FOUR_FOUR
                }
                else -> {
                    DefensivePlaybook.THREE_THREE_FIVE
                }
            }

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
        val user =
            FCFBUser(
                username = username,
                coachName = coachName,
                discordTag = discordUsername,
                discordId = interaction.user.id.toString(),
                email = email,
                password = password,
                salt = "",
                position = position,
                offensivePlaybook = offensivePlaybook,
                defensivePlaybook = defensivePlaybook,
            )
        val registeredUser = authClient.registerUser(user)
        if (registeredUser == null) {
            response.respond { this.content = "User registration failed!" }
            Logger.error("${interaction.user.username} failed to register a new user")
        } else {
            response.respond { this.content = "User registered successfully!" }
            Logger.info("${interaction.user.username} successfully registered a new user")
        }
    }
}
