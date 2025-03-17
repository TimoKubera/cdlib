package de.deutschepost.sdm.cdlib.release.report.external.fortify

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class EngineData(
    @JacksonXmlProperty(localName = "EngineVersion")
    val fortifyVersion: String,

    @JacksonXmlProperty(localName = "RuleInfo")
    val ruleInfo: List<Rule>
) {
    data class Rule(
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        val id: String,
        @JacksonXmlProperty(localName = "MetaInfo")
        val metaInfo: List<Group>
    ) {
        val probability: Double by lazy {
            this.metaInfo.find { entry -> entry.name == "Probability" }?.content?.toDoubleOrNull() ?: 1.0
        }

        val accuracy: Double by lazy {
            this.metaInfo.find { entry -> entry.name == "Accuracy" }?.content?.toDoubleOrNull() ?: 1.0
        }

        val impact: Double by lazy {
            this.metaInfo.find { entry -> entry.name == "Impact" }?.content?.toDoubleOrNull() ?: 0.0
        }

        @JsonDeserialize(using = GroupDeserializer::class)
        data class Group(
            @field:JacksonXmlProperty(localName = "name", isAttribute = true)
            val name: String,
            @JsonProperty("Group")
            val content: String
        )
    }
}

internal class GroupDeserializer :
    JsonDeserializer<EngineData.Rule.Group>() {

    /*
    class to parse XML of the form:
    <Group name="altcategorySTIG3.5">None</Group>
     */

    override fun deserialize(
        jsonParser: com.fasterxml.jackson.core.JsonParser?,
        deserializationContext: com.fasterxml.jackson.databind.DeserializationContext?
    ): EngineData.Rule.Group {
        //advance to first value ('name'="altcategorySTIG3.5")
        jsonParser?.nextValue()
        //save value into first element
        val name: String? = jsonParser?.valueAsString
        //advance to second value (''=None)
        jsonParser?.nextValue()
        //save value into second element
        val content: String? = jsonParser?.valueAsString
        //Advance to next Token (</Group>) to make sure stream input stays sane
        while (jsonParser?.currentToken != JsonToken.END_OBJECT) {
            jsonParser?.nextToken()
        }
        return EngineData.Rule.Group(content = content ?: "", name = name ?: "")
    }
}
