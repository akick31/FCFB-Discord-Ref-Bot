package com.fcfb.discord.refbot.model.log

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

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

enum class MessageType(val description: String) {
    GAME_THREAD("Game Thread"),
    PRIVATE_MESSAGE("Private Message"),
}
