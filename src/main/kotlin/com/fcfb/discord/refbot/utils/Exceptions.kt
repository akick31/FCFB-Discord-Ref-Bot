package com.fcfb.discord.refbot.utils

class MissingPlatformIdException : Exception("No platform ID found")
class GameMessageFailedException(gameId: Int? = null) :
    Exception("Failed to send game message${gameId?.let { " for game $it" } ?: ""}")
class OffensiveNumberRequestFailedException(gameId: Int? = null) :
    Exception("Failed to send offensive number request${gameId?.let { " for game $it" } ?: ""}")
class DefensiveNumberRequestFailedException(gameId: Int? = null) :
    Exception("Failed to send defensive number request${gameId?.let { " for game $it" } ?: ""}")
class NumberConfirmationMessageFailedException(gameId: Int? = null) :
    Exception("Failed to send number confirmation message${gameId?.let { " for game $it" } ?: ""}")
class InvalidOffensiveNumberSubmissionException(gameId: Int? = null) :
    Exception("Invalid offensive number submission${gameId?.let { " for game $it" } ?: ""}")
class InvalidDefensiveNumberSubmissionException(gameId: Int? = null) :
    Exception("Invalid defensive number submission${gameId?.let { " for game $it" } ?: ""}")
class NoGameFoundException : Exception("No game found")
class NoWriteupFoundException : Exception("No writeup found")
class CouldNotDetermineCoachPossessionException(gameId: Int? = null) :
    Exception("Could not determine which coach has possession${gameId?.let { " for game $it" } ?: ""}")
class CouldNotDetermineTeamPossessionException(gameId: Int? = null) :
    Exception("Could not determine which team has possession${gameId?.let { " for game $it" } ?: ""}")
class NoMessageContentFoundException(scenario: String) : Exception("No message content found for $scenario")
class InvalidChannelTypeException : Exception("Channel is not a TextChannelThread")
class InvalidGameThreadException(gameId: Int? = null) : Exception("Could not find game thread${gameId?.let { " for game $it" } ?: ""}")
class InvalidCoinTossWinnerException(gameId: Int? = null) :
    Exception("Invalid coin toss winner${gameId?.let { " for game $it" } ?: ""}")
class InvalidPlayCallException(messageContent: String) : Exception("Invalid play call: $messageContent")