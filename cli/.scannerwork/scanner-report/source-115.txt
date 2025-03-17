package de.deutschepost.sdm.cdlib.release.report.external

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import de.deutschepost.sdm.cdlib.release.report.internal.Tool
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcDependencyLicenseEntry
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcLicenseEntry
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult

data class OslcNPMPluginTestResult(
    @get:JsonIgnore
    val licenseEntries: MutableMap<String, OslcNPMPluginTestResultEntry>,
) {
    @JsonAnySetter
    fun allSetter(fieldName: String, fieldValue: OslcNPMPluginTestResultEntry) {
        licenseEntries[fieldName] = fieldValue
    }
}

data class OslcNPMPluginTestResultEntry(
    val licenses: String = "",
    val repository: String = "",
    val publisher: String = "",
    val email: String = "",
    val path: String = "",
    val licenseFile: String = "",
)


fun OslcTestPreResult.Companion.from(
    oslcNPMPluginTestResult: OslcNPMPluginTestResult,
    uri: String
): OslcTestPreResult {
    val dependencies = oslcNPMPluginTestResult.licenseEntries.map { mapEntry ->
        val sanitizedLicense = mapEntry.value.licenses.removeSuffix("*")
        OslcDependencyLicenseEntry(
            dependencyName = mapEntry.key.substringBeforeLast('@'),
            dependencySources = listOf(mapEntry.value.repository),
            dependencyVersion = mapEntry.key.substringAfterLast('@'),
            licenses = listOf(
                OslcLicenseEntry(
                    license = sanitizedLicense,
                    url = "",
                )
            )
        )
    }
    return OslcTestPreResult(
        tool = Tool(
            name = Tool.OSLC_NPM_PLUGIN_NAME,
            version = "",
            vendor = "",
            ruleVersion = ""
        ),
        uri = uri,
        dependencyLicenseEntries = dependencies
    )
}
