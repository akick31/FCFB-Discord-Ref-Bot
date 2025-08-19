package com.fcfb.discord.refbot.config.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fcfb.discord.refbot.config.jackson.deserializers.ActualResultDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.CoachPositionDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.ConferenceDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.DefensivePlaybookDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.GameModeDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.GameStatusDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.GameTypeDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.MessageTypeDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.OffensivePlaybookDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.PlatformDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.PlayCallDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.PlayTypeDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.RunoffTypeDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.ScenarioDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.SubdivisionDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.TVChannelDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.TeamSideDeserializer
import com.fcfb.discord.refbot.config.jackson.deserializers.UserRoleDeserializer
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.game.TVChannel
import com.fcfb.discord.refbot.model.enums.message.MessageType
import com.fcfb.discord.refbot.model.enums.play.ActualResult
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.play.RunoffType
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.model.enums.team.Conference
import com.fcfb.discord.refbot.model.enums.team.DefensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.OffensivePlaybook
import com.fcfb.discord.refbot.model.enums.team.Subdivision
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.model.enums.user.CoachPosition
import com.fcfb.discord.refbot.model.enums.user.UserRole

class JacksonConfig {
    private fun customGameModule(): SimpleModule {
        return SimpleModule().apply {
            addDeserializer(ActualResult::class.java, ActualResultDeserializer())
            addDeserializer(CoachPosition::class.java, CoachPositionDeserializer())
            addDeserializer(Conference::class.java, ConferenceDeserializer())
            addDeserializer(DefensivePlaybook::class.java, DefensivePlaybookDeserializer())
            addDeserializer(GameMode::class.java, GameModeDeserializer())
            addDeserializer(GameStatus::class.java, GameStatusDeserializer())
            addDeserializer(GameType::class.java, GameTypeDeserializer())
            addDeserializer(OffensivePlaybook::class.java, OffensivePlaybookDeserializer())
            addDeserializer(Platform::class.java, PlatformDeserializer())
            addDeserializer(PlayCall::class.java, PlayCallDeserializer())
            addDeserializer(PlayType::class.java, PlayTypeDeserializer())
            addDeserializer(UserRole::class.java, UserRoleDeserializer())
            addDeserializer(RunoffType::class.java, RunoffTypeDeserializer())
            addDeserializer(Scenario::class.java, ScenarioDeserializer())
            addDeserializer(Subdivision::class.java, SubdivisionDeserializer())
            addDeserializer(TeamSide::class.java, TeamSideDeserializer())
            addDeserializer(TVChannel::class.java, TVChannelDeserializer())
        }
    }

    fun configureGameMapping(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(customGameModule())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    fun configureApiResponseMapping(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    private fun customFCFBUserModule(): SimpleModule {
        return SimpleModule().apply {
            addDeserializer(CoachPosition::class.java, CoachPositionDeserializer())
            addDeserializer(UserRole::class.java, UserRoleDeserializer())
        }
    }

    fun configureFCFBUserMapping(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(customFCFBUserModule())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    private fun customTeamModule(): SimpleModule {
        return SimpleModule().apply {
            addDeserializer(Conference::class.java, ConferenceDeserializer())
        }
    }

    fun configureTeamMapping(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(customTeamModule())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    fun configureRequestMessageLogMapping(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(customRequestMessageLogModule())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    private fun customRequestMessageLogModule(): SimpleModule {
        return SimpleModule().apply {
            addDeserializer(MessageType::class.java, MessageTypeDeserializer())
        }
    }
}
