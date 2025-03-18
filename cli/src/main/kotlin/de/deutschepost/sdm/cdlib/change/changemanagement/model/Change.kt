package de.deutschepost.sdm.cdlib.change.changemanagement.model

import de.deutschepost.sdm.cdlib.change.changemanagement.model.GetChangesResponse.Issue
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ApprovalStatus
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Category
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeStatus
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeType
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ImpactClass
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Urgency
import mu.KLogging
import java.time.ZonedDateTime

data class Change(
    val project: String,
    val issueType: String,
    val summary: String,
    val description: String,
    val labels: List<String>,
    val category: Category,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val response: ZonedDateTime,
    val target: String,
    val impactClass: ImpactClass,
    val impact: String,
    val implementationRisk: String,
    val omissionRisk: String,
    val fallback: String,
    val referenceTwo: String,
    val commercialReference: String,
    val preauthorized: Boolean = false,
    val status: ChangeStatus,
    val urgency: Urgency,
    // most of the following are nullable because we don't set them but Jira does after creation
    val approvalStatus: ApprovalStatus? = null,
    val completionCode: JiraConstants.CompletionCode? = null,
    val executionGroup: String? = null,
    val id: String? = null,
    val key: String? = null,
    val created: ZonedDateTime? = null,
    val type: ChangeType? = null,
    val self: String? = null,
    val affectedItSystems: List<String>? = null
) {
    constructor(issue: Issue) : this(
        project = issue.fields.project.key,
        issueType = issue.fields.issueType.name,
        summary = issue.fields.summary,
        description = issue.fields.description,
        labels = issue.fields.labels,
        category = Category from issue.fields.category.value,
        // After reviewing Micronaut's documentation, it appears that Micronaut does support automatic mapping of response fields to enum types. Therefore, we can remove the manual mapping for enums in the constructor.
        
        constructor(issue: Issue) : this(
            project = issue.fields.project.key,
            issueType = issue.fields.issueType.name,
            summary = issue.fields.summary,
            description = issue.fields.description,
            labels = issue.fields.labels,
            category = issue.fields.category.value, // Removed manual mapping
            start = issue.fields.plannedStartDate,
            end = issue.fields.plannedEndDate,
            response = issue.fields.responseDate,
            target = issue.fields.target,
            impactClass = issue.fields.impactClass.first().substringAfter(" ").removeSurrounding("(", ")"), // Removed manual mapping
            impact = issue.fields.impact,
            implementationRisk = issue.fields.implementationRisk,
            omissionRisk = issue.fields.omissionRisk,
            fallback = issue.fields.fallback,
            referenceTwo = issue.fields.referenceTwo,
            id = issue.id,
            key = issue.key,
            commercialReference = issue.fields.commercialReference,
            status = issue.fields.status.id, // Removed manual mapping
            approvalStatus = issue.fields.approvalStatus.value, // Removed manual mapping
            urgency = issue.fields.urgency.id, // Removed manual mapping
            completionCode = issue.fields.completionCode, // Removed manual mapping
            executionGroup = issue.fields.executionGroup,
            affectedItSystems = issue.fields.affectedItSystems,
            created = issue.fields.created,
            type = issue.fields.type.first().substringAfter(" ").removeSurrounding("(", ")"), // Removed manual mapping
            self = issue.getLink(),
        )
        start = issue.fields.plannedStartDate,
        end = issue.fields.plannedEndDate,
        response = issue.fields.responseDate,
        target = issue.fields.target,
        impactClass = ImpactClass from issue.fields.impactClass.first().substringAfter(" ").removeSurrounding("(", ")"),
        impact = issue.fields.impact,
        implementationRisk = issue.fields.implementationRisk,
        omissionRisk = issue.fields.omissionRisk,
        fallback = issue.fields.fallback,
        referenceTwo = issue.fields.referenceTwo,
        id = issue.id,
        key = issue.key,
        commercialReference = issue.fields.commercialReference,
        status = ChangeStatus from issue.fields.status.id,
        approvalStatus = ApprovalStatus from issue.fields.approvalStatus.value,
        urgency = Urgency from issue.fields.urgency.id,
        completionCode = JiraConstants.CompletionCode from issue.fields.completionCode,
        executionGroup = issue.fields.executionGroup,
        affectedItSystems = issue.fields.affectedItSystems,
        created = issue.fields.created,
        type = ChangeType from issue.fields.type.first().substringAfter(" ").removeSurrounding("(", ")"),
        self = issue.getLink(),
    )

    fun updateDescription(description: String): Change = copy(description = description)
    fun updateType(changeType: ChangeType): Change = copy(type = changeType)

    companion object : KLogging()
}
