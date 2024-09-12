package zebstrika.model.play

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import zebstrika.model.game.PlayCall
import zebstrika.model.game.Result
import zebstrika.model.game.ActualResult
import zebstrika.model.game.TeamSide

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Play(
    @JsonProperty("play_id") val playId: Int,
    @JsonProperty("game_id") val gameId: Int?,
    @JsonProperty("play_number") val playNumber: Int?,
    @JsonProperty("home_score") val homeScore: Int?,
    @JsonProperty("away_score") val awayScore: Int?,
    @JsonProperty("game_quarter") val gameQuarter: Int?,
    @JsonProperty("clock") val clock: Int?,
    @JsonProperty("ball_location") val ballLocation: Int?,
    @JsonProperty("possession") val possession: TeamSide?,
    @JsonProperty("down") val down: Int?,
    @JsonProperty("yards_to_go") val yardsToGo: Int?,
    @JsonProperty("defensive_number") val defensiveNumber: String?,
    @JsonProperty("offensive_number") val offensiveNumber: String?,
    @JsonProperty("defensive_submitter") val defensiveSubmitter: String?,
    @JsonProperty("offensive_submitter") val offensiveSubmitter: String?,
    @JsonProperty("play_call") val playCall: PlayCall?,
    @JsonProperty("result") val result: Result?,
    @JsonProperty("difference") val difference: Int?,
    @JsonProperty("actual_result") val actualResult: ActualResult?,
    @JsonProperty("yards") val yards: Int?,
    @JsonProperty("play_time") val playTime: Int?,
    @JsonProperty("runoff_time") val runoffTime: Int?,
    @JsonProperty("win_probability") val winProbability: Double?,
    @JsonProperty("home_team") val homeTeam: String?,
    @JsonProperty("away_team") val awayTeam: String?,
    @JsonProperty("timeout_used") val timeoutUsed: Boolean?,
    @JsonProperty("home_timeouts") val homeTimeouts: Int?,
    @JsonProperty("away_timeouts") val awayTimeouts: Int?,
    @JsonProperty("play_finished") val playFinished: Boolean?
)