package com.fcfb.discord.refbot.model.discord

import com.fcfb.discord.refbot.utils.Logger

class MessageConstants {
    enum class Error(val message: String) {
        NO_GAME_FOUND("Could not find a game associated with this user."),
        WAITING_FOR_NUMBER_IN_DMS("This game is currently waiting on a number from you in your DMs"),
        WAITING_FOR_COIN_TOSS("This game is currently waiting on the away coach to call **heads** or **tails**"),
        WAITING_FOR_COIN_TOSS_CHOICE("This game is currently waiting on the coin toss winning coach to call **receive** or **defer**"),
        NOT_WAITING_FOR_USER("This game is not currently waiting on you to submit a number"),
        MULTIPLE_NUMBERS_FOUND("Please only include one number in your message, multiple were found."),
        INVALID_NUMBER("Could not find a valid number in the message. Please include a valid number between 1 and 1500."),
        INVALID_PLAY("Could not find a valid play in the message"),
        INVALID_OFFENSIVE_SUBMITTER("Could not find the offensive user's info"),
        INVALID_OFFENSIVE_SUBMISSION("There was an issue submitting the offensive number"),
        INVALID_DEFENSIVE_SUBMITTER("Could not find the defensive user's info"),
        INVALID_DEFENSIVE_SUBMISSION("There was an issue submitting the defensive number"),
        INVALID_COIN_TOSS("There was an issue handling the coin toss"),
        INVALID_COIN_TOSS_CHOICE("There was an issue handling the coin toss choice"),
        INVALID_COIN_TOSS_WINNER("Could not find user info for the winner of the coin toss"),
        INVALID_GAME_THREAD("Could not find the game thread. Please make sure the game thread exists and is accessible."),
        NO_WRITEUP_FOUND("There was an issue getting the writeup message"),
        GAME_THREAD_MESSAGE_EXCEPTION("Could not send message to game thread via message object or text channel object"),
        PRIVATE_MESSAGE_EXCEPTION("Could not send private message to the user"),
        ;

        fun logError() {
            Logger.error(message)
        }
    }

    enum class Info(val message: String) {
        COIN_TOSS("Coin toss has been completed"),
        COIN_TOSS_CHOICE("Coin toss choice has been made. The game is ready to start"),
        COIN_TOSS_OUTCOME("%s win the coin toss! Please choose whether you want to **receive** or **defer**."),
        SUCCESSFUL_NUMBER_SUBMISSION("I've got %s as your number"),
        MESSAGE_CONTAINS_TIMEOUT("The message contains 'timeout'"),
        MESSAGE_DOES_NOT_CONTAIN_TIMEOUT("The message does not contain 'timeout'"),
        MESSAGE_CONTAINS_RUN("The message contains 'run'"),
        MESSAGE_CONTAINS_PASS("The message contains 'pass'"),
        MESSAGE_CONTAINS_SPIKE("The message contains 'spike'"),
        MESSAGE_CONTAINS_KNEEL("The message contains 'kneel'"),
        MESSAGE_CONTAINS_PUNT("The message contains 'punt'"),
        MESSAGE_CONTAINS_FIELD_GOAL("The message contains 'field goal'"),
        MESSAGE_CONTAINS_PAT("The message contains 'pat'"),
        MESSAGE_CONTAINS_TWO_POINT("The message contains 'two point'"),
        MESSAGE_CONTAINS_NORMAL("The message contains 'normal'"),
        MESSAGE_CONTAINS_SQUIB("The message contains 'squib'"),
        MESSAGE_CONTAINS_ONSIDE("The message contains 'onside'"),
        MESSAGE_CONTAINS_HURRY("The message contains 'hurry'"),
        MESSAGE_CONTAINS_CHEW("The message contains 'chew'"),
        ;

        fun logInfo() {
            Logger.info(message)
        }
    }
}
