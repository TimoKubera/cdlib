package de.deutschepost.sdm.cdlib.change.metrics.model

import java.time.ZonedDateTime
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import de.deutschepost.sdm.cdlib.change.changemanagement.model.Change
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.change.metrics.DEPLOYMENT_PREFIX
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import de.deutschepost.sdm.cdlib.utils.resolveEnvByNameSanitized
import mu.KLogging
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Deployment(
    val id: String,
    val app: String,
    @JsonProperty("icto")
    val almId: String,
    @JsonProperty("product")
    val itSystem: String,
    val deploymentType: String,
    val deployedToProduction: Boolean,
    val release: Release?,
    val cdlibData: CdlibVersionViewModel,
    val status: String,
    val date: ZonedDateTime = ZonedDateTime.now(),
    val tool: PipelineToolData,
    val url: String,
    val webapproval: Webapproval?,
    val deploymentLeadTimeInSeconds: Long?,
    val hasWebapproval: Boolean,
    val hasTqs: Boolean,
    val hasOslc: Boolean,
    val change: DeploymentChangeViewModel,
    @JsonProperty("environment")
    val environment: String?,
    @JsonProperty("version")
    val version: String?
) : Jsonable {
    constructor(
        cdlibData: CdlibVersionViewModel,
        deploymentType: String,
data class Deployment(
    val id: String,
    val app: String,
    @JsonProperty("icto")
    val almId: String,
    @JsonProperty("product")
    val itSystem: String,
    val deploymentType: String,
    val deployedToProduction: Boolean,
    val release: Release?,
    val cdlibData: CdlibVersionViewModel,
    val status: String,
    val date: ZonedDateTime = ZonedDateTime.now(),
    val tool: PipelineToolData,
    val url: String,
    val webapproval: Webapproval?,
    val deploymentLeadTimeInSeconds: Long?,
    val hasWebapproval: Boolean,
    val hasTqs: Boolean,
    val hasOslc: Boolean,
    val change: DeploymentChangeViewModel
) : Jsonable {
    constructor(
        cdlibData: CdlibVersionViewModel,
        deploymentType: String,
        release: Release?,
        status: Status = Status.FAILURE,
        itSystem: ItSystem,
        change: Change,
        gitops: Boolean,
        deploymentLeadTimeInSeconds: Long?,
        webapproval: Webapproval? = null,
        hasWebapproval: Boolean = false,
        hasTqs: Boolean = false,
        hasOslc: Boolean = false,
        isTest: Boolean = false
    ) : this(
        id = createId(itSystem.name),
        app = resolveEnvByName(name = Names.CDLIB_APP_NAME),
        almId = itSystem.almId,
        itSystem = if (isTest) {
            "TEST"
        } else {
            itSystem.name
        },
        deploymentType = deploymentType,
        deployedToProduction = true,
        release = release,
        cdlibData = cdlibData,
        status = status.value,
        tool = PipelineToolData(),
        url = resolveEnvByName(Names.CDLIB_JOB_URL),
        webapproval = webapproval,
        deploymentLeadTimeInSeconds = deploymentLeadTimeInSeconds,
        hasWebapproval = hasWebapproval,
        hasTqs = hasTqs,
        hasOslc = hasOslc,
        change = DeploymentChangeViewModel(change, itSystem, gitops)
    )

    @get:JsonIgnore
    override val canonicalFilename: String
        get() = buildString {
            append(DEPLOYMENT_PREFIX)
            append("_")
            append(resolveEnvByNameSanitized(Names.CDLIB_RELEASE_NAME))
            append("_")
            append(date.format(DateTimeFormatter.ISO_INSTANT).replace(":", "-"))
            append(".json")
        }


    @Suppress("unused")
    enum class Status(val value: String) {
        // Jenkins
        SUCCESS(STATUS_SUCCESS),
        FAILURE(STATUS_FAILURE),
        ABORTED(STATUS_ABORTED),
        UNSTABLE(STATUS_UNSTABLE),

        // Azure DevOps
        SUCCEEDED(STATUS_SUCCESS),
        FAILED(STATUS_FAILURE),
        CANCELED(STATUS_ABORTED),
        SUCCEEDEDWITHISSUES(STATUS_UNSTABLE),
        ;
    }

    enum class DeploymentType {
        APP, INFRA
    }

    companion object : KLogging() {
        const val STATUS_FAILURE = "FAILURE"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_ABORTED = "ABORTED"
        const val STATUS_UNSTABLE = "UNSTABLE"

        private fun createId(itSystem: String): String {
            fun String.sanitize() = replace("[^\\p{Alnum}-.+_]".toRegex(), "_")

            val sanitizedRelease = resolveEnvByName(Names.CDLIB_RELEASE_NAME).sanitize()
            val sanitizedItSystem = itSystem.sanitize()
            val timestamp = System.currentTimeMillis()
            return "${sanitizedItSystem}_${sanitizedRelease}_$timestamp"
        }
    }
}
