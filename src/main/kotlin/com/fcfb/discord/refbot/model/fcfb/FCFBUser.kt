package com.fcfb.discord.refbot.model.fcfb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fcfb.discord.refbot.model.fcfb.Role.USER
import com.fcfb.discord.refbot.model.fcfb.game.DefensivePlaybook
import com.fcfb.discord.refbot.model.fcfb.game.OffensivePlaybook

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FCFBUser(
    @JsonProperty("id") var id: Long? = 0,
    @JsonProperty("username") var username: String,
    @JsonProperty("coach_name") var coachName: String,
    @JsonProperty("discord_tag") var discordTag: String,
    @JsonProperty("discord_id") var discordId: String,
    @JsonProperty("email") var email: String?,
    @JsonProperty("password") var password: String?,
    @JsonProperty("position") var position: CoachPosition,
    @JsonProperty("role") var role: Role = USER,
    @JsonProperty("salt") var salt: String?,
    @JsonProperty("team") var team: String? = null,
    @JsonProperty("delay_of_game_instances") var delayOfGameInstances: Int = 0,
    @JsonProperty("wins") var wins: Int = 0,
    @JsonProperty("losses") var losses: Int = 0,
    @JsonProperty("win_percentage") var winPercentage: Double = 0.0,
    @JsonProperty("conference_wins") var conferenceWins: Int = 0,
    @JsonProperty("conference_losses") var conferenceLosses: Int = 0,
    @JsonProperty("conference_championship_wins") var conferenceChampionshipWins: Int = 0,
    @JsonProperty("conference_championship_losses") var conferenceChampionshipLosses: Int = 0,
    @JsonProperty("bowl_wins") var bowlWins: Int = 0,
    @JsonProperty("bowl_losses") var bowlLosses: Int = 0,
    @JsonProperty("playoff_wins") var playoffWins: Int = 0,
    @JsonProperty("playoff_losses") var playoffLosses: Int = 0,
    @JsonProperty("national_championship_wins") var nationalChampionshipWins: Int = 0,
    @JsonProperty("national_championship_losses") var nationalChampionshipLosses: Int = 0,
    @JsonProperty("offensive_playbook") var offensivePlaybook: OffensivePlaybook?,
    @JsonProperty("defensive_playbook") var defensivePlaybook: DefensivePlaybook?,
    @JsonProperty("approved") var approved: Byte?,
    @JsonProperty("verification_token") var verificationToken: String?,
)

enum class CoachPosition(val description: String) {
    HEAD_COACH("Head Coach"),
    OFFENSIVE_COORDINATOR("Offensive Coordinator"),
    DEFENSIVE_COORDINATOR("Defensive Coordinator"),
    RETIRED("Retired"),
}

enum class Role(val description: String) {
    USER("User"),
    CONFERENCE_COMMISSIONER("Conference Commissioner"),
    ADMIN("Admin"),
}
