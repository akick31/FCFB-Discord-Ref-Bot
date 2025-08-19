package com.fcfb.discord.refbot.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PagedResponse<T>(
    @JsonProperty("content") val content: List<T>,
    @JsonProperty("pageable") val pageable: Pageable,
    @JsonProperty("last") val last: Boolean,
    @JsonProperty("total_pages") val totalPages: Int,
    @JsonProperty("total_elements") val totalElements: Int,
    @JsonProperty("first") val first: Boolean,
    @JsonProperty("size") val size: Int,
    @JsonProperty("number") val number: Int,
    @JsonProperty("sort") val sort: Sort,
    @JsonProperty("number_of_elements") val numberOfElements: Int,
    @JsonProperty("empty") val empty: Boolean,
)

data class Pageable(
    @JsonProperty("sort") val sort: Sort,
    @JsonProperty("offset") val offset: Int,
    @JsonProperty("page_number") val pageNumber: Int,
    @JsonProperty("page_size") val pageSize: Int,
    @JsonProperty("paged") val paged: Boolean,
    @JsonProperty("unpaged") val unpaged: Boolean,
)

data class Sort(
    @JsonProperty("empty") val empty: Boolean,
    @JsonProperty("sorted") val sorted: Boolean,
    @JsonProperty("unsorted") val unsorted: Boolean,
)
