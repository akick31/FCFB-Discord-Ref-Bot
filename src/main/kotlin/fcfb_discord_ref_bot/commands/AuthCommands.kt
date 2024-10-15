package fcfb_discord_ref_bot.commands

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import fcfb_discord_ref_bot.api.AuthClient
import fcfb_discord_ref_bot.model.fcfb.game.DefensivePlaybook
import fcfb_discord_ref_bot.model.fcfb.game.OffensivePlaybook
import fcfb_discord_ref_bot.model.fcfb.CoachPosition
import fcfb_discord_ref_bot.model.fcfb.User
import utils.Logger

class AuthCommands {
    suspend fun registerUser(
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand,
    ) {
        Logger.info("${interaction.user.username} is registering a new user")
        val response = interaction.deferEphemeralResponse()
        val username = command.strings["username"]!!
        val coachName = command.strings["coach_name"]!!
        val discordUsername = interaction.user.username
        val email = command.options["email"]!!
        val password = command.options["password"]!!
        val positionString = command.options["position"]!!.value.toString()
        val redditUsername = command.options["reddit_username"]
        val offensivePlaybookString = command.options["offensive_playbook"]!!.value.toString()
        val defensivePlaybookString = command.options["defensive_playbook"]!!.value.toString()

        val offensivePlaybook = if (offensivePlaybookString == OffensivePlaybook.AIR_RAID.toString()) {
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
        val defensivePlaybook = when (defensivePlaybookString) {
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
        val user = User(
            username = username,
            coachName = coachName,
            discordTag = discordUsername,
            email = email.value.toString(),
            password = password.value.toString(),
            salt = "",
            position = position,
            redditUsername = redditUsername?.value.toString(),
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            verificationToken = ""
        )
        val registeredUser = AuthClient().registerUser(user)
        if (registeredUser == null) {
            response.respond { this.content = "User registration failed!" }
            Logger.error("${interaction.user.username} failed to register a new user")
        } else {
            response.respond { this.content = "User registered successfully!" }
            Logger.info("${interaction.user.username} successfully registered a new user")
        }
    }
}