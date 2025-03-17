package de.deutschepost.sdm.cdlib.change.changemanagement.model

import com.fasterxml.jackson.annotation.JsonIgnore
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.AFFECTED_IT_SYSTEMS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.APPROVAL_STATUS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.CATEGORY
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMMERCIAL_REFERENCE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMPLETION_CODE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.CREATED
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.DESCRIPTION
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.EXECUTION_GROUP
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.FALLBACK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPACT
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPACT_CLASS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPLEMENTATION_RISK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.ISSUE_TYPE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IT_SYSTEM
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.LABELS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.OMISSION_RISK
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PLANNED_END_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PLANNED_START_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PROJECT
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.REFERENCE_TWO
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.RESPONSE_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.STATUS
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.SUMMARY
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.TARGET
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.TYPE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.URGENCY

class GetChangesRequest(
    @JsonIgnore
    val referenceTwo: String,
    itSystemKey: String?,
    isTest: Boolean
) {
    val fields: List<String> =
        listOf(
            LABELS,
            STATUS,
            CREATED,
            DESCRIPTION,
            PROJECT,
            SUMMARY,
            ISSUE_TYPE,
            IT_SYSTEM,
            COMMERCIAL_REFERENCE,
            IMPACT_CLASS,
            CATEGORY,
            PLANNED_START_DATE,
            PLANNED_END_DATE,
            RESPONSE_DATE,
            REFERENCE_TWO,
            TARGET,
            IMPACT,
            IMPLEMENTATION_RISK,
            OMISSION_RISK,
            FALLBACK,
            TYPE,
            APPROVAL_STATUS,
            URGENCY,
            COMPLETION_CODE,
            EXECUTION_GROUP,
            AFFECTED_IT_SYSTEMS
        )
    val jql = buildString {
        append("project = ${if (isTest) JiraConstants.FieldDefaults.TEST_PROJECT else JiraConstants.FieldDefaults.PROD_PROJECT} AND ")
        append("issuetype = Change AND ")
        append("status IN ('In Progress', 'Awaiting Implementation', 'Waiting For Approval') AND ")
        append("labels IN ('cdlib') AND ")
        append("cf[${REFERENCE_TWO.substringAfter("_")}] ~ '\"$referenceTwo\"' ")
        if (!itSystemKey.isNullOrBlank()) {
            append("AND cf[${IT_SYSTEM.substringAfter("_")}] IN ('$itSystemKey')")
        }
    }

    @Suppress("unused")
    val maxResults: Int = -1

    @Suppress("unused")
    val startAt: Int = 0
}
