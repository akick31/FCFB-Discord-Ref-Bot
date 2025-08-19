package com.fcfb.discord.refbot.model.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fcfb.discord.refbot.model.enums.message.MessageType

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RequestMessageLog(
    @JsonProperty("id") val id: Long? = 0,
    @JsonProperty("message_type") val messageType: MessageType,
    @JsonProperty("game_id") val gameId: Int,
    @JsonProperty("play_id") val playId: Int?,
    @JsonProperty("message_id") val messageId: Long,
    @JsonProperty("message_location") val messageLocation: String?,
    @JsonProperty("message_ts") val messageTs: String,
)
