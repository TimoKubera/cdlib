package de.deutschepost.sdm.cdlib.release.report.external

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StringDeserializer
import de.deutschepost.sdm.cdlib.release.report.internal.Tool
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcDependencyLicenseEntry
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcLicenseEntry
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult

data class OslcGradlePluginTestResult(
    val dependencies: List<OslcGradlePluginTestResultEntry>
)


data class OslcGradlePluginTestResultEntry(
    val moduleName: String = "",
    val moduleUrls: List<String> = listOf(""),
    val moduleVersion: String = "",
    val moduleLicenses: List<OslcGradlePluginTestResultLicenseEntry> = listOf(
        OslcGradlePluginTestResultLicenseEntry(moduleLicense = "UNKNOWN", moduleLicenseUrl = "")
    )
)


data class OslcGradlePluginTestResultLicenseEntry(
    @JsonDeserialize(using = EmptyStringWhenNull::class)
    val moduleLicense: String = "",
    @JsonDeserialize(using = EmptyStringWhenNull::class)
    val moduleLicenseUrl: String = ""

) {
    companion object {
        object EmptyStringWhenNull : StringDeserializer() {
            override fun getNullValue(context: DeserializationContext): String = "NULL"
        }
    }

}

fun OslcDependencyLicenseEntry.Companion.from(
    entry: OslcGradlePluginTestResultEntry,
): OslcDependencyLicenseEntry {
    return OslcDependencyLicenseEntry(
        dependencyName = entry.moduleName,
        dependencySources = entry.moduleUrls,
        licenses = entry.moduleLicenses.mapNotNull {
            when {
                it.moduleLicenseUrl.contains(',') -> null

                it.moduleLicense == "NULL" -> null

                else -> OslcLicenseEntry(license = it.moduleLicense, url = it.moduleLicenseUrl)
            }
        },
        dependencyVersion = entry.moduleVersion
    )
}

fun OslcTestPreResult.Companion.from(
    oslcGradlePluginTestResult: OslcGradlePluginTestResult,
    uri: String
): OslcTestPreResult {
    val dependencyLicenseEntries = oslcGradlePluginTestResult.dependencies.map { OslcDependencyLicenseEntry.from(it) }
    return OslcTestPreResult(
        tool = Tool(
            name = Tool.OSLC_GRADLE_PLUGIN_NAME,
            version = "",
            vendor = "",
            ruleVersion = ""
        ),
        uri = uri,
        dependencyLicenseEntries = dependencyLicenseEntries
    )
}

