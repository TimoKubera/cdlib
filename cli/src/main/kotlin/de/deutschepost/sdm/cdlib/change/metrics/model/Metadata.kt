package de.deutschepost.sdm.cdlib.change.metrics.model

import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName

data class PipelineToolData(
    val name: String = resolveEnvByName(Names.CDLIB_CICD_PLATFORM),
    val instance: String = trimToBaseUrl(),
    val url: String = resolveEnvByName(Names.CDLIB_JOB_URL)
) {
    companion object {
        private fun trimToBaseUrl(): String {
            val url = resolveEnvByName(Names.CDLIB_JOB_URL)
            val protocol = url.substringBefore("//")
            val domain = url.substringAfter("$protocol//").substringBefore("/")
            val baseUrl = "$protocol//$domain"

            return if (domain == "dev.azure.com") {
                val saasInstance = url.substringAfter("$baseUrl/").substringBefore("/")
                "$baseUrl/$saasInstance"
            } else {
                baseUrl
            }
        }
    }
}

data class Commit(
    val authorMail: String,
    val authorName: String,
    val id: String,
    val link: String,
    val message: String,
    val repository: String
) {
    constructor() : this(
        authorMail = resolveEnvByName(Names.CDLIB_PM_GIT_MAIL),
        authorName = resolveEnvByName(Names.CDLIB_PM_GIT_NAME),
        id = resolveEnvByName(Names.CDLIB_PM_GIT_ID),
        link = resolveEnvByName(Names.CDLIB_PM_GIT_LINK),
        message = resolveEnvByName(Names.CDLIB_PM_GIT_MESSAGE),
        repository = resolveEnvByName(Names.CDLIB_PM_GIT_ORIGIN)
    )
}
