package fcfb_discord_ref_bot.model.fcfb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import fcfb_discord_ref_bot.model.fcfb.game.DefensivePlaybook
import fcfb_discord_ref_bot.model.fcfb.game.OffensivePlaybook
import fcfb_discord_ref_bot.model.fcfb.game.Subdivision

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Team (
    @JsonProperty("id") var id: Long? = 0,
    @JsonProperty("logo") var logo: String? = null,
    @JsonProperty("coach_username1") var coachUsername1: String? = null,
    @JsonProperty("coach_name1") var coachName1: String? = null,
    @JsonProperty("coach_discord_tag1") var coachDiscordTag1: String? = null,
    @JsonProperty("coach_discord_id1") var coachDiscordId1: String? = null,
    @JsonProperty("coach_username2") var coachUsername2: String? = null,
    @JsonProperty("coach_name2") var coachName2: String? = null,
    @JsonProperty("coach_discord_tag2") var coachDiscordTag2: String? = null,
    @JsonProperty("coach_discord_id2") var coachDiscordId2: String? = null,
    @JsonProperty("coaches_poll_ranking") var coachesPollRanking: Int? = null,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("playoff_committee_ranking") var playoffCommitteeRanking: Int? = null,
    @JsonProperty("abbreviation") var abbreviation: String? = null,
    @JsonProperty("primary_color") var primaryColor: String? = null,
    @JsonProperty("secondary_color") var secondaryColor: String? = null,
    @JsonProperty("subdivision") var subdivision: Subdivision? = null,
    @JsonProperty("offensive_playbook") var offensivePlaybook: OffensivePlaybook? = null,
    @JsonProperty("defensive_playbook") var defensivePlaybook: DefensivePlaybook? = null,
    @JsonProperty("conference") var conference: Conference? = null,
    @JsonProperty("current_wins") var currentWins: Int? = null,
    @JsonProperty("current_losses") var currentLosses: Int? = null,
    @JsonProperty("overall_wins") var overallWins: Int? = null,
    @JsonProperty("overall_losses") var overallLosses: Int? = null,
    @JsonProperty("current_conference_wins") var currentConferenceWins: Int? = null,
    @JsonProperty("current_conference_losses") var currentConferenceLosses: Int? = null,
    @JsonProperty("overall_conference_wins") var overallConferenceWins: Int? = null,
    @JsonProperty("overall_conference_losses") var overallConferenceLosses: Int? = null
)

enum class Conference(val description: String) {
    ACC("ACC"),
    AMERICAN("American"),
    BIG_12("Big 12"),
    BIG_TEN("Big Ten"),
    CUSA("C-USA"),
    FBS_INDEPENDENT("FBS Independent"),
    MAC("MAC"),
    MOUNTAIN_WEST("Mountain West"),
    PAC_12("Pac-12"),
    SEC("SEC"),
    SUN_BELT("Sun Belt"),
    ATLANTIC_SUN("Atlantic Sun"),
    BIG_SKY("Big Sky"),
    CAROLINA_FOOTBALL_CONFERENCE("Carolina Football Conference"),
    MISSOURI_VALLEY("Missouri Valley"),
    COLONIAL("Colonial"),
    NEC("NEC"),
    IVY_LEAGUE("Ivy League"),
    MID_ATLANTIC("Mid-Atlantic"),
    SOUTHLAND("Southland"),
    OHIO_VALLEY("Ohio Valley"),
    SWAC("SWAC");

    companion object {
        fun fromString(description: String): Conference? {
            return Conference.values().find { it.description == description }
        }
    }
}