package de.deutschepost.sdm.cdlib.change.changemanagement.model


import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.AFFECTED_IT_SYSTEMS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.APPROVAL_STATUS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.CATEGORY
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMMERCIAL_REFERENCE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMPLETION_CODE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.EXECUTION_GROUP
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
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.TYPE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.URGENCY
import java.time.ZonedDateTime

data class GetChangesResponse(
    @JsonProperty("issues")
    val issues: List<Issue>,
    @JsonProperty("maxResults")
    val maxResults: Int,
    @JsonProperty("startAt")
    val startAt: Int,
    @JsonProperty("total")
    val total: Int
) {
    data class Issue(
        @JsonProperty("expand")
        val expand: String,
        @JsonProperty("fields")
        val fields: Fields,
        @JsonProperty("id")
        val id: String,
        @JsonProperty("key")
        override val key: String,
    ) : HumanReadableLink {
        data class Fields(
            @JsonProperty(CATEGORY)
            val category: Category,
            @JsonProperty(FALLBACK)
            val fallback: String,
            @JsonProperty(IMPACT)
            val impact: String,
            @JsonProperty(IMPACT_CLASS)
            val impactClass: List<String>,
            @JsonProperty(IMPLEMENTATION_RISK)
            val implementationRisk: String,
            @JsonProperty(ISSUE_TYPE)
            val issueType: IssueType,
            @JsonProperty(OMISSION_RISK)
            val omissionRisk: String,
            val project: Project,
            val labels: List<String>,
            val status: Status,
            val created: ZonedDateTime,
            val description: String,
            @JsonProperty(TYPE)
            val type: List<String>,
            @JsonProperty(PLANNED_START_DATE)
            val plannedStartDate: ZonedDateTime,
            @JsonProperty(PLANNED_END_DATE)
            val plannedEndDate: ZonedDateTime,
            @JsonProperty(REFERENCE_TWO)
            val referenceTwo: String,
            @JsonProperty(RESPONSE_DATE)
            val responseDate: ZonedDateTime,
            val summary: String,
            @JsonProperty(TARGET)
            val target: String,
            @JsonProperty(COMMERCIAL_REFERENCE)
            val commercialReference: String,
            @JsonProperty(APPROVAL_STATUS)
            val approvalStatus: ApprovalStatusField,
            @JsonProperty(URGENCY)
            val urgency: UrgencyField,
            @JsonProperty(COMPLETION_CODE)
            val completionCode: CompletionCodeField?,
            @JsonProperty(EXECUTION_GROUP)
            val executionGroup: String,
            @JsonProperty(AFFECTED_IT_SYSTEMS)
            val affectedItSystems: List<String>,
        ) {
            data class Category(val value: String)
            data class IssueType(val name: String)
            data class Project(val key: String)
            data class Status(
                val id: String,
                val name: String
            )

            data class ApprovalStatusField(val value: String)
            data class UrgencyField(val value: String? = null, val id: String)
            data class CompletionCodeField(val value: String, val id: String)
        }
    }
}
