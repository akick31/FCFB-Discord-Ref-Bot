package com.fcfb.discord.refbot.model.request

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SignupInfo(
    val discordTag: String,
    val discordId: String,
    val teamChoiceOne: String,
    val teamChoiceTwo: String,
    val teamChoiceThree: String,
)
