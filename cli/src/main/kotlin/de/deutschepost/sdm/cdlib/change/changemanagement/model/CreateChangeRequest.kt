package de.deutschepost.sdm.cdlib.change.changemanagement.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.changemanagement.model.GetChangesResponse.Issue
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.CATEGORY
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMMERCIAL_REFERENCE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.FALLBACK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPACT
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPACT_CLASS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPLEMENTATION_RISK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.ISSUE_TYPE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.OMISSION_RISK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PLANNED_END_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PLANNED_START_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.REFERENCE_TWO
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.RESPONSE_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.TARGET
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.URGENCY
import java.time.ZonedDateTime

data class CreateChangeRequest(
    val project: Issue.Fields.Project,
    @JsonProperty(ISSUE_TYPE)
    val issueType: Issue.Fields.IssueType?,
    val summary: String,
    val description: String,
    val labels: List<String>,
    @JsonProperty(COMMERCIAL_REFERENCE)
    val commercialReference: List<CommercialReference>,
    @JsonProperty(CATEGORY)
    val category: Issue.Fields.Category,
    @JsonProperty(PLANNED_START_DATE)
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val plannedStartDate: ZonedDateTime,
    @JsonProperty(PLANNED_END_DATE)
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val plannedEndDate: ZonedDateTime,
    @JsonProperty(RESPONSE_DATE)
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val responseTime: ZonedDateTime,
    @JsonProperty(TARGET)
    val target: String,
    @JsonProperty(IMPACT_CLASS)
    val impactClass: List<ImpactClass>,
    @JsonProperty(IMPACT)
    val impact: String,
    @JsonProperty(IMPLEMENTATION_RISK)
    val implementationRisk: String,
    @JsonProperty(OMISSION_RISK)
    val omissionRisk: String,
    @JsonProperty(FALLBACK)
    val fallback: String,
    @JsonProperty(REFERENCE_TWO)
    val referenceTwo: String,
    @JsonProperty(URGENCY)
    val urgency: Issue.Fields.UrgencyField,
) {
    constructor(change: Change) : this(
        project = Issue.Fields.Project(change.project),
        issueType = Issue.Fields.IssueType(change.issueType),
        summary = change.summary,
        description = change.description,
        labels = change.labels,
        commercialReference = listOf(CommercialReference(change.commercialReference)),
        category = Issue.Fields.Category(change.category.value),
        plannedStartDate = change.start,
        plannedEndDate = change.end,
        responseTime = change.response,
        target = change.target,
        impactClass = listOf(ImpactClass(change.impactClass.value)),
        impact = change.impact,
        implementationRisk = change.implementationRisk,
        omissionRisk = change.omissionRisk,
        fallback = change.fallback,
        referenceTwo = change.referenceTwo,
        urgency = Issue.Fields.UrgencyField(id = change.urgency.value)
    )

    data class ImpactClass(val key: String)
    data class CommercialReference(val key: String)
}
