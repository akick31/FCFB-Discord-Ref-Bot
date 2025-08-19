package com.fcfb.discord.refbot.model.enums.message

enum class Info(val message: String) {
    COIN_TOSS_OUTCOME("%s won the coin toss! Please reply to this message with **receive** or **defer**."),
    OVERTIME_COIN_TOSS_OUTCOME("%s won the coin toss! Please reply to this message with **offense** or **defense**."),
    SUCCESSFUL_NUMBER_SUBMISSION("I've got %s as your number"),
}
