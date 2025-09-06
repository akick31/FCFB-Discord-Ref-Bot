package com.fcfb.discord.refbot.model.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fcfb.discord.refbot.model.enums.team.Conference
import com.fcfb.discord.refbot.model.enums.team.DefensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.OffensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.Subdivision

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Team(
    @JsonProperty("id") var id: Long = 0,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("short_name") var location: String? = null,
    @JsonProperty("abbreviation") var abbreviation: String? = null,
    @JsonProperty("logo") var logo: String? = null,
    @JsonProperty("scorebug_logo") var scorebugLogo: String? = null,
    @JsonProperty("coach_usernames") var coachUsernames: List<String>? = null,
    @JsonProperty("coach_names") var coachNames: List<String>? = null,
    @JsonProperty("coach_discord_tags") var coachDiscordTags: List<String>? = null,
    @JsonProperty("coach_discord_ids") var coachDiscordIds: List<String>? = null,
    @JsonProperty("primary_color") var primaryColor: String? = null,
    @JsonProperty("secondary_color") var secondaryColor: String? = null,
    @JsonProperty("coaches_poll_ranking") var coachesPollRanking: Int? = null,
    @JsonProperty("playoff_committee_ranking") var playoffCommitteeRanking: Int? = null,
    @JsonProperty("subdivision") var subdivision: Subdivision? = null,
    @JsonProperty("offensive_playbook") var offensivePlaybook: OffensivePlaybook? = null,
    @JsonProperty("defensive_playbook") var defensivePlaybook: DefensivePlaybook? = null,
    @JsonProperty("conference") var conference: Conference? = null,
    @JsonProperty("current_wins") var currentWins: Int = 0,
    @JsonProperty("current_losses") var currentLosses: Int = 0,
    @JsonProperty("overall_wins") var overallWins: Int = 0,
    @JsonProperty("overall_losses") var overallLosses: Int = 0,
    @JsonProperty("current_conference_wins") var currentConferenceWins: Int = 0,
    @JsonProperty("current_conference_losses") var currentConferenceLosses: Int = 0,
    @JsonProperty("overall_conference_wins") var overallConferenceWins: Int = 0,
    @JsonProperty("overall_conference_losses") var overallConferenceLosses: Int = 0,
    @JsonProperty("conference_championship_wins") var conferenceChampionshipWins: Int = 0,
    @JsonProperty("conference_championship_losses") var conferenceChampionshipLosses: Int = 0,
    @JsonProperty("bowl_wins") var bowlWins: Int = 0,
    @JsonProperty("bowl_losses") var bowlLosses: Int = 0,
    @JsonProperty("playoff_wins") var playoffWins: Int = 0,
    @JsonProperty("playoff_losses") var playoffLosses: Int = 0,
    @JsonProperty("national_championship_wins") var nationalChampionshipWins: Int = 0,
    @JsonProperty("national_championship_losses") var nationalChampionshipLosses: Int = 0,
    @JsonProperty("is_taken") var isTaken: Boolean = false,
    @JsonProperty("active") var active: Boolean = true,
    @JsonProperty("current_elo") var currentElo: Double,
    @JsonProperty("overall_elo") var overallElo: Double,
)
