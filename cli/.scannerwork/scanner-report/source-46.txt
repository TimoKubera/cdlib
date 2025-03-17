package de.deutschepost.sdm.cdlib.change.changemanagement.model

import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Criticality


data class ItSystem(
    val commercialReferenceNumber: String,
    val name: String,
    val key: String,
    val almId: String,
    val criticality: Criticality,
    val businessYear: Int
)

data class ItSystemResponse(
    val objectEntries: List<JiraObjectEntry>,
    val objectTypeAttributes: List<JiraObjectTypeAttribute>,
) {
    fun mapItSystemInfo(): ItSystem {
        val itSystemAttributeId = objectTypeAttributes
            .find { x -> x.name == "IT-System/-Applikation ID" }
            ?.id
            ?: throw Exception("Could not find IT system attribute id.")

        val almAttributeId = objectTypeAttributes
            .find { x -> x.name == "ALM-ID" }
            ?.id
            ?: throw Exception("Could not find ALM-ID attribute id.")

        val businessYearId = objectTypeAttributes
            .find { x -> x.name == "Geschäftsjahr" }
            ?.id
            ?: throw Exception("Could not find business year attribute id.")

        return objectEntries.map {
            val almId = it.attributes
                ?.find { x -> x.objectTypeAttributeId == almAttributeId }
                ?.objectAttributeValues?.get(0)
                ?: throw Exception("Could not find ALM-ID for object ${it.objectKey}.")

            val itSystem = it.attributes
                .find { x -> x.objectTypeAttributeId == itSystemAttributeId }
                ?.objectAttributeValues?.get(0)
                ?: throw Exception("Could not find IT System for object ${it.objectKey}.")

            val businessYearValue = it.attributes
                .find { x -> x.objectTypeAttributeId == businessYearId }
                ?.objectAttributeValues?.get(0)
                ?: throw Exception("Could not find business year value for object ${it.objectKey}.")

            val businessCriticality = itSystem.referencedObject?.attributes
                ?.find { x ->
                    x.objectAttributeValues
                        ?.any { y -> y.referencedObject?.objectType?.name == "Business Kritikalität" } == true
                }?.objectAttributeValues?.get(0)
                ?: throw Exception("Could not find business criticality for object ${it.objectKey}.")

            ItSystem(
                name = itSystem.displayValue.substringBefore("|").trim(),
                key = itSystem.searchValue,
                almId = almId.displayValue,
                commercialReferenceNumber = it.objectKey,
                criticality = Criticality from businessCriticality.displayValue,
                businessYear = businessYearValue.searchValue.toInt()
            )
        }.maxByOrNull {
            it.businessYear
        } ?: throw Exception("Could not find any IT system for the given commercial reference.")
    }
}

data class JiraObjectEntry(
    val attributes: List<JiraAttribute>?,
    val objectType: JiraObjectType,
    val objectKey: String,
    val label: String
)

data class JiraObjectTypeAttribute(
    val id: Int,
    val name: String,
)

data class JiraAttribute(
    val objectAttributeValues: List<JiraObjectAttributeValue>?,
    val objectTypeAttributeId: Int
)

data class JiraObjectAttributeValue(
    val displayValue: String,
    val referencedObject: JiraObjectEntry?,
    val searchValue: String,
)

data class JiraObjectType(
    val name: String,
    val parentObjectTypeId: Int,
)
