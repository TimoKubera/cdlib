package de.deutschepost.sdm.cdlib.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.ConfigFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.util.JacksonFeature
import com.fasterxml.jackson.databind.json.JsonMapper


private val sharedConfiguration = listOf<Pair<ConfigFeature, Boolean>>(
    Pair(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false),
)

private val permissiveConfiguration = listOf<Pair<ConfigFeature, Boolean>>(
    Pair(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
) + sharedConfiguration

private val permissiveFeatures = listOf<JacksonFeature>(
    JsonReadFeature.ALLOW_TRAILING_COMMA
)

private val sharedModules = arrayOf(
    JavaTimeModule()
)

val defaultObjectMapper: ObjectMapper =
    jacksonMapperBuilder().build().applyConfig(sharedConfiguration).applyModules()

val permissiveObjectMapper: ObjectMapper =
    jacksonMapperBuilder().enableFeatures(permissiveFeatures).build().applyConfig(permissiveConfiguration)
        .applyModules()

val defaultXmlMapper: ObjectMapper =
    XmlMapper().applyConfig(permissiveConfiguration).registerKotlinModule().applyModules()

private fun ObjectMapper.applyModules(): ObjectMapper = this.registerModules(*sharedModules)

private fun ObjectMapper.applyConfig(config: List<Pair<ConfigFeature, Boolean>>): ObjectMapper {
    for ((feature, flag) in config) {
        when (feature) {
            is SerializationFeature -> configure(feature, flag)
            is DeserializationFeature -> configure(feature, flag)
        }
    }
    return this
}

private fun JsonMapper.Builder.enableFeatures(features: List<JacksonFeature>): JsonMapper.Builder {
    for (feature in features) {
        when (feature) {
            is JsonReadFeature -> enable(feature)
        }
    }
    return this
}
