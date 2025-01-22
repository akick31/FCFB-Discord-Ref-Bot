package com.fcfb.discord.refbot.model.log

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RequestMessageLog(
    @JsonProperty("message_type") val messageType: MessageType,
    @JsonProperty("game_id") val gameId: Int,
    @JsonProperty("play_id") val playId: Int?,
    @JsonProperty("message_id") val messageId: Int,
    @JsonProperty("message_content") val messageContent: String,
    @JsonProperty("message_location") val messageLocation: String?,
    @JsonProperty("message_ts") val messageTs: String,
)

enum class MessageType(val description: String) {
    OFFENSE("Offense"),
    DEFENSE("Defense"),
}
