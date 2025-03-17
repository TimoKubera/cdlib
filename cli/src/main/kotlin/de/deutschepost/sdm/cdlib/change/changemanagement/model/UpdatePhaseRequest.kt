package de.deutschepost.sdm.cdlib.change.changemanagement.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.COMPLETION_CODE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.DEVIATION
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.DEVIATION_CAUSE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPLEMENTATION_END
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldNames.IMPLEMENTATION_START
import java.time.ZonedDateTime

data class UpdateChangePhaseRequest(
    val transition: JiraTransition,
    val fields: JiraTransitionFields? = null,
    val update: JiraTransitionUpdate? = null
)

data class JiraTransition(val id: Int)

data class JiraTransitionFields(
    @JsonProperty(COMPLETION_CODE) // :{"value": "Erfolgreich implementiert"}
    val completionCodeField: CompletionCodeField,
    @JsonProperty(IMPLEMENTATION_START) // "2021-12-02T13:17:21.161+0000" this needs to be time of approval
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val startOfImplementation: ZonedDateTime,
    @JsonProperty(IMPLEMENTATION_END) // "2021-12-02T14:17:21.161+0000" this needs to be now
    @JsonFormat(pattern = JiraConstants.DATETIME_ISO8601)
    val endOfImplementation: ZonedDateTime,
    @JsonProperty(DEVIATION) // not there for successful
    val deviation: String? = null,
    @JsonProperty(DEVIATION_CAUSE) // not there for successful
    val deviationCause: String? = null,
)

data class CompletionCodeField(val value: String)

data class JiraTransitionUpdate(val comment: List<JiraComment>) {
    constructor(comment: String) : this(listOf(JiraComment(JiraAdd(comment))))
}

data class JiraComment(val add: JiraAdd)

data class JiraAdd(val body: String) // "Change wurde durchgeführt."
