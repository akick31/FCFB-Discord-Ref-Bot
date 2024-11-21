package com.fcfb.discord.refbot.model.fcfb.game

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

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
    @JsonProperty("home_wins") val homeWins: Int?,
    @JsonProperty("home_losses") val homeLosses: Int?,
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
    @JsonProperty("home_platform") val homePlatform: Platform,
    @JsonProperty("home_platform_id") val homePlatformId: String?,
    @JsonProperty("away_platform") val awayPlatform: Platform,
    @JsonProperty("away_platform_id") val awayPlatformId: String?,
    @JsonProperty("game_timer") val gameTimer: String?,
    @JsonProperty("current_play_type") val currentPlayType: PlayType?,
    @JsonProperty("current_play_id") val currentPlayId: Int?,
    @JsonProperty("clock_stopped") val clockStopped: Boolean,
    @JsonProperty("request_message_id") val requestMessageId: List<String>?,
    @JsonProperty("game_status") val gameStatus: GameStatus?,
    @JsonProperty("game_type") val gameType: GameType?,
)

enum class GameStatus(val description: String) {
    PREGAME("PREGAME"),
    OPENING_KICKOFF("OPENING KICKOFF"),
    IN_PROGRESS("IN PROGRESS"),
    HALFTIME("HALFTIME"),
    FINAL("FINAL"),
    END_OF_REGULATION("END OF REGULATION"),
    OVERTIME("OVERTIME"),
    ;
}

enum class Subdivision(val description: String) {
    FCFB("FCFB"),
    FBS("FBS"),
    FCS("FCS"),
    ;
}

enum class OffensivePlaybook(val description: String) {
    FLEXBONE("FLEXBONE"),
    AIR_RAID("AIR RAID"),
    PRO("PRO"),
    SPREAD("SPREAD"),
    WEST_COAST("WEST COAST"),
    ;
}

enum class DefensivePlaybook(val description: String) {
    FOUR_THREE("4-3"),
    THREE_FOUR("3-4"),
    FIVE_TWO("5-2"),
    FOUR_FOUR("4-4"),
    THREE_THREE_FIVE("3-3-5"),
    ;
}

enum class TVChannel(val description: String) {
    ABC("ABC"),
    CBS("CBS"),
    ESPN("ESPN"),
    ESPN2("ESPN2"),
    FOX("FOX"),
    FS1("FS1"),
    FS2("FS2"),
    NBC("NBC"),
    ;
}

enum class Platform(val description: String) {
    DISCORD("DISCORD"),
    REDDIT("REDDIT"),
    ;
}

enum class PlayCall(val description: String) {
    RUN("RUN"),
    PASS("PASS"),
    SPIKE("SPIKE"),
    KNEEL("KNEEL"),
    FIELD_GOAL("FIELD_GOAL"),
    PUNT("PUNT"),
    PAT("PAT"),
    TWO_POINT("TWO_POINT"),
    KICKOFF_NORMAL("KICKOFF_NORMAL"),
    KICKOFF_ONSIDE("KICKOFF_ONSIDE"),
    KICKOFF_SQUIB("KICKOFF_SQUIB"),
    ;
}

enum class PlayType(val description: String) {
    NORMAL("NORMAL"),
    KICKOFF("KICKOFF"),
    PAT("PAT"),
    ;
}

enum class ActualResult(val description: String) {
    FIRST_DOWN("FIRST DOWN"),
    GAIN("GAIN"),
    NO_GAIN("NO GAIN"),
    TURNOVER_ON_DOWNS("TURNOVER ON DOWNS"),
    TOUCHDOWN("TOUCHDOWN"),
    SAFETY("SAFETY"),
    TURNOVER("TURNOVER"),
    TURNOVER_TOUCHDOWN("TURNOVER TOUCHDOWN"),
    KICKING_TEAM_TOUCHDOWN("KICKING TEAM TOUCHDOWN"),
    RETURN_TOUCHDOWN("KICKING TEAM TOUCHDOWN"),
    MUFFED_KICK("MUFFED KICK"),
    KICKOFF("KICKOFF"),
    SUCCESSFUL_ONSIDE("SUCCESSFUL ONSIDE"),
    FAILED_ONSIDE("FAILED ONSIDE"),
    GOOD("GOOD"),
    NO_GOOD("NO GOOD"),
    BLOCKED("BLOCKED"),
    KICK_SIX("KICK SIX"),
    DEFENSE_TWO_POINT("DEFENSE TWO POINT"),
    SUCCESS("SUCCESS"),
    FAILED("NO FAILED"),
    SPIKE("SPIKE"),
    KNEEL("KNEEL"),
    PUNT("PUNT"),
    PUNT_RETURN_TOUCHDOWN("PUNT RETURN TOUCHDOWN"),
    PUNT_TEAM_TOUCHDOWN("PUNT TEAM TOUCHDOWN"),
    MUFFED_PUNT("MUFFED PUNT"),
    ;
}

enum class RunoffType(val description: String) {
    CHEW("CHEW"),
    HURRY("HURRY"),
    NORMAL("NORMAL"),
    ;
}

enum class Scenario(val description: String) {
    GAME_START("GAME START"),
    PLAY_RESULT("PLAY RESULT"),
    COIN_TOSS("COIN_TOSS"),
    COIN_TOSS_CHOICE("COIN TOSS CHOICE"),
    KICKOFF_NUMBER_REQUEST("KICKOFF NUMBER REQUEST"),
    NORMAL_NUMBER_REQUEST("NORMAL NUMBER REQUEST"),
    DM_NUMBER_REQUEST("DM NUMBER REQUEST"),
    GOOD("GOOD"),
    NO_GOOD("NO GOOD"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    NO_GAIN("NO GAIN"),
    INCOMPLETE("INCOMPLETE"),
    LOSS_OF_10_YARDS("-10"),
    LOSS_OF_7_YARDS("-7"),
    LOSS_OF_5_YARDS("-5"),
    LOSS_OF_3_YARDS("-3"),
    LOSS_OF_2_YARDS("-2"),
    LOSS_OF_1_YARD("-1"),
    GAIN_OF_1_YARD("1"),
    GAIN_OF_2_YARDS("2"),
    GAIN_OF_3_YARDS("3"),
    GAIN_OF_4_YARDS("4"),
    GAIN_OF_5_YARDS("5"),
    GAIN_OF_6_YARDS("6"),
    GAIN_OF_7_YARDS("7"),
    GAIN_OF_8_YARDS("8"),
    GAIN_OF_9_YARDS("9"),
    GAIN_OF_10_YARDS("10"),
    GAIN_OF_11_YARDS("11"),
    GAIN_OF_12_YARDS("12"),
    GAIN_OF_13_YARDS("13"),
    GAIN_OF_14_YARDS("14"),
    GAIN_OF_15_YARDS("15"),
    GAIN_OF_16_YARDS("16"),
    GAIN_OF_17_YARDS("17"),
    GAIN_OF_18_YARDS("18"),
    GAIN_OF_19_YARDS("19"),
    GAIN_OF_20_YARDS("20"),
    GAIN_OF_25_YARDS("25"),
    GAIN_OF_30_YARDS("30"),
    GAIN_OF_35_YARDS("35"),
    GAIN_OF_40_YARDS("40"),
    GAIN_OF_45_YARDS("45"),
    GAIN_OF_50_YARDS("50"),
    GAIN_OF_55_YARDS("55"),
    GAIN_OF_60_YARDS("60"),
    GAIN_OF_65_YARDS("65"),
    GAIN_OF_70_YARDS("70"),
    GAIN_OF_75_YARDS("75"),
    GAIN_OF_80_YARDS("80"),
    TURNOVER_PLUS_20_YARDS("TO + 20 YARDS"),
    TURNOVER_PLUS_15_YARDS("TO + 15 YARDS"),
    TURNOVER_PLUS_10_YARDS("TO + 10 YARDS"),
    TURNOVER_PLUS_5_YARDS("TO + 5 YARDS"),
    TURNOVER("TO"),
    TURNOVER_MINUS_5_YARDS("TO - 5 YARDS"),
    TURNOVER_MINUS_10_YARDS("TO - 10 YARDS"),
    TURNOVER_MINUS_15_YARDS("TO - 15 YARDS"),
    TURNOVER_MINUS_20_YARDS("TO - 20 YARDS"),
    TURNOVER_ON_DOWNS("TURNOVER ON DOWNS"),
    TURNOVER_TOUCHDOWN("TURNOVER TOUCHDOWN"),
    TOUCHDOWN("TOUCHDOWN"),
    SAFETY("SAFETY"),
    FUMBLE("FUMBLE"),
    FIVE_YARD_RETURN("5 YARD RETURN"),
    TEN_YARD_RETURN("10 YARD RETURN"),
    TWENTY_YARD_RETURN("20 YARD RETURN"),
    THIRTY_YARD_RETURN("30 YARD RETURN"),
    THIRTY_FIVE_YARD_RETURN("35 YARD RETURN"),
    FORTY_YARD_RETURN("40 YARD RETURN"),
    FORTY_FIVE_YARD_RETURN("45 YARD RETURN"),
    FIFTY_YARD_RETURN("50 YARD RETURN"),
    SIXTY_FIVE_YARD_RETURN("65 YARD RETURN"),
    TOUCHBACK("TOUCHBACK"),
    RETURN_TOUCHDOWN("RETURN TOUCHDOWN"),
    RECOVERED("RECOVERED"),
    FAILED_ONSIDE("FAILED ONSIDE"),
    DEFENSE_TWO_POINT("DEFENSE TWO POINT"),
    SPIKE("SPIKE"),
    KNEEL("KNEEL"),
    BLOCKED_PUNT("BLOCKED PUNT"),
    PUNT_RETURN_TOUCHDOWN("PUNT RETURN TOUCHDOWN"),
    BLOCKED_FIELD_GOAL("BLOCKED FIELD GOAL"),
    KICK_SIX("KICK SIX"),
    FIVE_YARD_PUNT("5 YARD PUNT"),
    TEN_YARD_PUNT("10 YARD PUNT"),
    FIFTEEN_YARD_PUNT("15 YARD PUNT"),
    TWENTY_YARD_PUNT("20 YARD PUNT"),
    TWENTY_FIVE_YARD_PUNT("25 YARD PUNT"),
    THIRTY_YARD_PUNT("30 YARD PUNT"),
    THIRTY_FIVE_YARD_PUNT("35 YARD PUNT"),
    FORTY_YARD_PUNT("40 YARD PUNT"),
    FORTY_FIVE_YARD_PUNT("45 YARD PUNT"),
    FIFTY_YARD_PUNT("50 YARD PUNT"),
    FIFTY_FIVE_YARD_PUNT("55 YARD PUNT"),
    SIXTY_YARD_PUNT("60 YARD PUNT"),
    SIXTY_FIVE_YARD_PUNT("65 YARD PUNT"),
    SEVENTY_YARD_PUNT("70 YARD PUNT"),
    GAME_OVER("GAME OVER"),
    ;
}

enum class TeamSide(val description: String) {
    HOME("HOME"),
    AWAY("AWAY"),
    ;
}

enum class CoinTossChoice(val description: String) {
    RECEIVE("RECEIVE"),
    DEFER("DEFER"),
}

enum class GameType(val description: String) {
    OUT_OF_CONFERENCE("Out of Conference"),
    CONFERENCE_GAME("Conference Game"),
    CONFERENCE_CHAMPIONSHIP("Conference Championship"),
    PLAYOFFS("Playoffs"),
    NATIONAL_CHAMPIONSHIP("National Championship"),
    BOWL("Bowl"),
    SCRIMMAGE("Scrimmage"),
    ;
}
