package de.deutschepost.sdm.cdlib.change.changemanagement.model

interface HumanReadableLink {
    val key: String
    fun getLink(): String = "${JiraConstants.JIRA_API_URL_PROD}/jira1/browse/${key}"
}
