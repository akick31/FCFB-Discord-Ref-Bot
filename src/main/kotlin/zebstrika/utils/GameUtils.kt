package zebstrika.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import zebstrika.model.game.Game
import zebstrika.model.game.Possession

class GameUtils {

    fun convertBallLocationToText(
        game: Game
    ): String {
        val ballLocation = game.ballLocation!!

        return when {
            ballLocation > 50 && game.possession == Possession.HOME -> {
                "${game.awayTeam} ${100 - ballLocation}"
            }
            ballLocation > 50 && game.possession == Possession.AWAY -> {
                "${game.homeTeam} ${100 - ballLocation}"
            }
            ballLocation < 50 && game.possession == Possession.HOME -> {
                "${game.homeTeam} $ballLocation"
            }
            ballLocation < 50 && game.possession == Possession.AWAY -> {
                "${game.awayTeam} $ballLocation"
            }
            else -> "50"
        }
    }
}