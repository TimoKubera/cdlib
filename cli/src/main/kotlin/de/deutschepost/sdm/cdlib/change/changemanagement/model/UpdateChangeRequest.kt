package de.deutschepost.sdm.cdlib.change.changemanagement.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.PLANNED_END_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.RESPONSE_DATE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.TYPE
import java.time.ZonedDateTime

data class UpdateChangeRequest(
    @JsonProperty(TYPE)
    val changeType: List<ChangeType>? = null,
    val description: String? = null,
    @JsonProperty(PLANNED_END_DATE)
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val plannedEndDate: ZonedDateTime? = null,
    @JsonProperty(RESPONSE_DATE)
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val responseTime: ZonedDateTime? = null
) {
    constructor(change: Change) : this(
        changeType = if (change.type != null) {
            listOf(ChangeType(change.type.value))
        } else {
            null
        },
        description = change.description,
        plannedEndDate = change.end,
        responseTime = change.response
    )
}

data class ChangeType(val key: String)
