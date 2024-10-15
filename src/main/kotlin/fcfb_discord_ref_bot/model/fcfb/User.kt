package fcfb_discord_ref_bot.model.fcfb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import fcfb_discord_ref_bot.model.fcfb.Role.USER
import fcfb_discord_ref_bot.model.fcfb.game.DefensivePlaybook
import fcfb_discord_ref_bot.model.fcfb.game.OffensivePlaybook

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class User (
    @JsonProperty("id") var id: Long? = 0,
    @JsonProperty("username") var username: String,
    @JsonProperty("coach_name") var coachName: String,
    @JsonProperty("discord_tag") var discordTag: String,
    @JsonProperty("discord_id") var discordId: String? = null,
    @JsonProperty("email") var email: String,
    @JsonProperty("password") var password: String,
    @JsonProperty("position") var position: CoachPosition,
    @JsonProperty("reddit_username") var redditUsername: String? = null,
    @JsonProperty("role") var role: Role? = USER,
    @JsonProperty("salt") var salt: String,
    @JsonProperty("team") var team: String? = null,
    @JsonProperty("wins") var wins: Int? = 0,
    @JsonProperty("losses") var losses: Int? = 0,
    @JsonProperty("win_percentage") var winPercentage: Double? = 0.0,
    @JsonProperty("offensive_playbook") var offensivePlaybook: OffensivePlaybook? = null,
    @JsonProperty("defensive_playbook") var defensivePlaybook: DefensivePlaybook? = null,
    @JsonProperty("approved") var approved: Byte? = 0,
    @JsonProperty("verification_token") var verificationToken: String
)

enum class CoachPosition(val description: String) {
    HEAD_COACH("Head Coach"),
    OFFENSIVE_COORDINATOR("Offensive Coordinator"),
    DEFENSIVE_COORDINATOR("Defensive Coordinator"),
    RETIRED("Retired");

    companion object {
        fun fromString(description: String): CoachPosition? {
            return CoachPosition.values().find { it.description == description }
        }
    }
}

enum class Role(val description: String) {
    USER("User"),
    CONFERENCE_COMMISSIONER("Conference Commissioner"),
    ADMIN("Admin");

    companion object {
        fun fromString(description: String): Role? {
            return Role.values().find { it.description == description }
        }
    }
}
