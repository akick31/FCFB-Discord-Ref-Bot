package com.fcfb.discord.refbot.model.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Conference(
    @JsonProperty("code") var code: String,
    @JsonProperty("label") var label: String,
    @JsonProperty("logo_url") var logoUrl: String? = null,
    @JsonProperty("logo_url_dark") var logoUrlDark: String? = null,
    @JsonProperty("abbreviation") var abbreviation: String? = null,
    @JsonProperty("active") var active: Boolean = true,
    @JsonProperty("championship_venue") var championshipVenue: String? = null,
)
