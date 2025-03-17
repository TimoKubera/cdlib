package de.deutschepost.sdm.cdlib.change.sharepoint.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import io.micronaut.jackson.annotation.JacksonFeatures

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
@JacksonFeatures(
    disabledDeserializationFeatures = [DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES]
)
data class SharepointOslcListItem(
    val appName: String,
    @JsonProperty("ITSystemName") val itSystemName: String,
    @JsonProperty("Title") val almId: String,
    val releaseVersion: String = resolveEnvByName(Names.CDLIB_APP_VERSION),
    val reportURL: String,
    val pipelineURL: String = resolveEnvByName(Names.CDLIB_JOB_URL),
    val distribution: Boolean,
    @JsonProperty("Prueftiefe") val depth: String,
    val tool: String,
    @JsonProperty("Compliancestatus") val compliance: String,
    @JsonProperty("Lizenzliste") val licenseList: String?
) : SharepointListItem {
    @JsonIgnore
    override val listNameProd = "OSLC_Ergebnisse"

    @JsonIgnore
    override val listNameTest = "OSLC_Ergebnisse_TEST"

    constructor(
        appName: String,
        itSystem: ItSystem,
        reportUrl: String,
        oslcTestResult: OslcTestResult,
        isDistribution: Boolean
    ) : this(
        appName = appName,
        itSystemName = itSystem.name,
        almId = itSystem.almId,
        reportURL = reportUrl,
        distribution = isDistribution,
        depth = when (oslcTestResult.depth) {
            "libs" -> "Bibliotheken"
            "libs+code" -> "Bibliotheken und Sourcecode"
            else -> throw IllegalArgumentException("OSLC depth should be either 'libs' or 'libs+code'")
        },
        tool = oslcTestResult.tool.name,
        compliance = when (oslcTestResult.complianceStatus) {
            OslcComplianceStatus.GREEN -> "Gruen"
            OslcComplianceStatus.YELLOW -> "Gelb"
            OslcComplianceStatus.RED -> "Rot"
        },
        licenseList = oslcTestResult.uniqueLicenses.joinToString("\n"),
    )

}
