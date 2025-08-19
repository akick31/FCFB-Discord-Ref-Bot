package com.fcfb.discord.refbot.model.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SignupInfoDTO(
    val discordTag: String,
    val discordId: String,
    val teamChoiceOne: String,
    val teamChoiceTwo: String,
    val teamChoiceThree: String,
)
