package com.fcfb.discord.refbot.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fcfb.discord.refbot.config.deserializers.ActualResultDeserializer
import com.fcfb.discord.refbot.config.deserializers.CoachPositionDeserializer
import com.fcfb.discord.refbot.config.deserializers.ConferenceDeserializer
import com.fcfb.discord.refbot.config.deserializers.DefensivePlaybookDeserializer
import com.fcfb.discord.refbot.config.deserializers.GameModeDeserializer
import com.fcfb.discord.refbot.config.deserializers.GameStatusDeserializer
import com.fcfb.discord.refbot.config.deserializers.GameTypeDeserializer
import com.fcfb.discord.refbot.config.deserializers.MessageTypeDeserializer
import com.fcfb.discord.refbot.config.deserializers.OffensivePlaybookDeserializer
import com.fcfb.discord.refbot.config.deserializers.PlatformDeserializer
import com.fcfb.discord.refbot.config.deserializers.PlayCallDeserializer
import com.fcfb.discord.refbot.config.deserializers.PlayTypeDeserializer
import com.fcfb.discord.refbot.config.deserializers.RoleDeserializer
import com.fcfb.discord.refbot.config.deserializers.RunoffTypeDeserializer
import com.fcfb.discord.refbot.config.deserializers.ScenarioDeserializer
import com.fcfb.discord.refbot.config.deserializers.SubdivisionDeserializer
import com.fcfb.discord.refbot.config.deserializers.TVChannelDeserializer
import com.fcfb.discord.refbot.config.deserializers.TeamSideDeserializer
import com.fcfb.discord.refbot.model.fcfb.CoachPosition
import com.fcfb.discord.refbot.model.fcfb.Conference
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.DefensivePlaybook
import com.fcfb.discord.refbot.model.fcfb.game.GameMode
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.OffensivePlaybook
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayType
import com.fcfb.discord.refbot.model.fcfb.game.RunoffType
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.model.log.MessageType

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
            addDeserializer(Role::class.java, RoleDeserializer())
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
            addDeserializer(Role::class.java, RoleDeserializer())
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
