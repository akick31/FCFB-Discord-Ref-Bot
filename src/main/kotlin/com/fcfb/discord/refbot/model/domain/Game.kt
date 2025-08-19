package com.fcfb.discord.refbot.model.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.game.GameWarning
import com.fcfb.discord.refbot.model.enums.game.TVChannel
import com.fcfb.discord.refbot.model.enums.gameflow.CoinTossChoice
import com.fcfb.discord.refbot.model.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.model.enums.team.DefensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.OffensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.Subdivision
import com.fcfb.discord.refbot.model.enums.team.TeamSide

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Game(
    @JsonProperty("game_id") val gameId: Int,
    @JsonProperty("home_team") val homeTeam: String,
    @JsonProperty("away_team") val awayTeam: String,
    @JsonProperty("home_coaches") val homeCoaches: List<String>,
    @JsonProperty("away_coaches") val awayCoaches: List<String>,
    @JsonProperty("home_coach_discord_ids") val homeCoachDiscordIds: List<String>,
    @JsonProperty("away_coach_discord_ids") val awayCoachDiscordIds: List<String>,
    @JsonProperty("home_offensive_playbook") val homeOffensivePlaybook: OffensivePlaybook?,
    @JsonProperty("away_offensive_playbook") val awayOffensivePlaybook: OffensivePlaybook?,
    @JsonProperty("home_defensive_playbook") val homeDefensivePlaybook: DefensivePlaybook?,
    @JsonProperty("away_defensive_playbook") val awayDefensivePlaybook: DefensivePlaybook?,
    @JsonProperty("home_score") val homeScore: Int,
    @JsonProperty("away_score") val awayScore: Int,
    @JsonProperty("possession") val possession: TeamSide,
    @JsonProperty("quarter") val quarter: Int,
    @JsonProperty("clock") val clock: String,
    @JsonProperty("ball_location") val ballLocation: Int,
    @JsonProperty("down") val down: Int,
    @JsonProperty("yards_to_go") val yardsToGo: Int,
    @JsonProperty("tv_channel") val tvChannel: TVChannel?,
    @JsonProperty("start_time") val startTime: String?,
    @JsonProperty("location") val location: String?,
    @JsonProperty("home_team_rank") val homeTeamRank: Int?,
    @JsonProperty("home_wins") val homeWins: Int?,
    @JsonProperty("home_losses") val homeLosses: Int?,
    @JsonProperty("away_team_rank") val awayTeamRank: Int?,
    @JsonProperty("away_wins") val awayWins: Int?,
    @JsonProperty("away_losses") val awayLosses: Int?,
    @JsonProperty("subdivision") val subdivision: Subdivision?,
    @JsonProperty("timestamp") val timestamp: String?,
    @JsonProperty("win_probability") val winProbability: Double?,
    @JsonProperty("season") val season: Int?,
    @JsonProperty("week") val week: Int?,
    @JsonProperty("waiting_on") val waitingOn: TeamSide,
    @JsonProperty("num_plays") val numPlays: Int,
    @JsonProperty("home_timeouts") val homeTimeouts: Int,
    @JsonProperty("away_timeouts") val awayTimeouts: Int,
    @JsonProperty("coin_toss_winner") val coinTossWinner: TeamSide?,
    @JsonProperty("coin_toss_choice") val coinTossChoice: CoinTossChoice?,
    @JsonProperty("overtime_coin_toss_winner") val overtimeCoinTossWinner: TeamSide?,
    @JsonProperty("overtime_coin_toss_choice") val overtimeCoinTossChoice: OvertimeCoinTossChoice?,
    @JsonProperty("home_platform") val homePlatform: Platform,
    @JsonProperty("home_platform_id") val homePlatformId: String?,
    @JsonProperty("away_platform") val awayPlatform: Platform,
    @JsonProperty("away_platform_id") val awayPlatformId: String?,
    @JsonProperty("last_message_timestamp") val lastMessageTimestamp: String?,
    @JsonProperty("game_timer") val gameTimer: String?,
    @JsonProperty("game_warning") val gameWarning: GameWarning?,
    @JsonProperty("current_play_type") val currentPlayType: PlayType?,
    @JsonProperty("current_play_id") val currentPlayId: Int?,
    @JsonProperty("clock_stopped") val clockStopped: Boolean,
    @JsonProperty("request_message_id") val requestMessageId: List<String>?,
    @JsonProperty("game_status") var gameStatus: GameStatus?,
    @JsonProperty("game_type") val gameType: GameType?,
    @JsonProperty("game_mode") val gameMode: GameMode?,
    @JsonProperty("overtime_half") val overtimeHalf: Int?,
    @JsonProperty("close_game") val closeGame: Boolean,
    @JsonProperty("close_game_pinged") val closeGamePinged: Boolean,
    @JsonProperty("upset_alert") val upsetAlert: Boolean,
    @JsonProperty("upset_alert_pinged") val upsetAlertPinged: Boolean,
)
