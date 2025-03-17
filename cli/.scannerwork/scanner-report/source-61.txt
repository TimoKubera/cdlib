package de.deutschepost.sdm.cdlib.change.metrics.model

import de.deutschepost.sdm.cdlib.change.changemanagement.model.Change
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem

data class DeploymentChangeViewModel(
    val jiraID: String,
    val jiraChangeTitle: String,
    val jiraURL: String,
    val type: String,
    val approvalStatus: String,
    val businessCriticality: String,
    val impactClass: String,
    val commercialReference: String,
    val itSystem: String,
    val almID: String,
    val category: String,
    val urgency: String,
    val description: String,
    val externalReference2: String,
    val executionGroup: String,
    val completionCode: String?,
    val affectedItSystems: List<String>?,
    val riskDetails: RiskDetails,
    val gitops: Boolean
) {
    constructor(change: Change, itSystem: ItSystem, gitops: Boolean) : this(
        jiraID = change.key ?: throw UnsupportedOperationException("Change id is null."),
        jiraChangeTitle = change.summary,
        jiraURL = change.self ?: throw UnsupportedOperationException("Change link is null."),
        type = change.type?.name ?: throw UnsupportedOperationException("Change type is null."),
        approvalStatus = change.approvalStatus?.value
            ?: throw UnsupportedOperationException("Change approvalStatus is null."),
        businessCriticality = itSystem.criticality.value,
        impactClass = change.impactClass.name,
        commercialReference = change.commercialReference,
        itSystem = itSystem.name,
        almID = itSystem.almId,
        category = change.category.name,
        urgency = change.urgency.name,
        description = change.description,
        externalReference2 = change.referenceTwo,
        executionGroup = change.executionGroup ?: throw UnsupportedOperationException("Change executionGroup is null."),
        completionCode = change.completionCode?.value,
        affectedItSystems = change.affectedItSystems,
        riskDetails = RiskDetails(
            target = change.target,
            impact = change.impact,
            implementationRisk = change.implementationRisk,
            omissionRisk = change.omissionRisk,
            fallback = change.fallback
        ),
        gitops = gitops
    )
}

data class RiskDetails(
    val target: String,
    val impact: String,
    val implementationRisk: String,
    val omissionRisk: String,
    val fallback: String
)
