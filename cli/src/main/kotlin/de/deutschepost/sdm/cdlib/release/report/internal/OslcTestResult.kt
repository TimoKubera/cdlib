package de.deutschepost.sdm.cdlib.release.report.internal

import kotlin.Unit
import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KLogging
import java.time.ZonedDateTime

data class OsclLicenseReportEntry(
    val license: String,
    val url: String,
    val count: Int
)

data class OslcTestResult(
    override val tool: Tool,
    override val uri: String,
    override val date: ZonedDateTime = ZonedDateTime.now(),
    override val reportType: ReportType = ReportType.OSLC,
    val uniqueLicenses: List<String>,
    val projectName: String,
    val projectId: Int,
    val depth: String,
    val policyProfile: String,
    // unapprovedItems content from FNCI: License -> ( Approval-Status -> list of inventory items )
    val unapprovedItems: Map<String, Map<String, List<String>>>,
    val acceptedItems: List<String> = emptyList(),
    val totalArtifactCount: Int,
    val complianceStatus: OslcComplianceStatus
) : TestResult() {

    fun isValid(): Boolean = when {
        complianceStatus == OslcComplianceStatus.GREEN && unapprovedItems.isNotEmpty() -> {
            logger.error("OSLC cannot be GREEN with unapproved Items!")
            false
        }

        complianceStatus == OslcComplianceStatus.RED -> {
            logger.error { "OSLC Compliance is red! (Rot)" }
            false
        }

        complianceStatus == OslcComplianceStatus.YELLOW -> {
            logger.warn { "OSLC Compliance is yellow! (Gelb)" }
            true
        }

        complianceStatus == OslcComplianceStatus.GREEN -> {
            true
        }

        else -> throw IllegalStateException("How did we get here?")
    }

    fun printInfos(appName: String) {
        buildList {
            add("OSLC Results for $appName:")
            add("Policy Profile: $policyProfile")
            add("Compliance Status: $complianceStatus ")
            add("Tool: ${tool.name}")
            add("Depth: $depth")
            add("Total Artifacts: $totalArtifactCount")
            if (uniqueLicenses.isNotEmpty()) {
                add("Unique Licenses:")
                addAll(uniqueLicenses.map { " - $it" })
            }
            add("Unapproved Licenses Count: ${unapprovedItems.size}")
            if (unapprovedItems.isNotEmpty()) {
                add("Unapproved Licenses details:")
                addAll(unapprovedItems.map { " - ${it.key}: ${it.value.values.flatten()}" })
            }
            if (acceptedItems.isNotEmpty()) {
                add("acceptedItems: [${acceptedItems.joinToString(separator = ", ")}]")
                add("acceptedItemsCount: ${acceptedItems.size}")
            }

        }.forEach {
            logger.info { "[$appName] $it" }
        }
    }

    @JsonIgnore
    override val canonicalIdentifier: String = projectName.replace("\\s+".toRegex(), "-")

    companion object : KLogging() {
        const val PROFILE_DISTRIBUTION = "Distribution"
        const val PROFILE_NON_DISTRIBUTION = "Non-Distribution"
        const val PROFILE_PLUGIN = "P&P OSLC Profile"
    }
}

fun List<OslcTestResult>.oslcTestsVerify(appName: String, isDistribution: Boolean) {
    val issues = mutableListOf<String>()
    this.forEach { test ->
        test.printInfos(appName)
        if (!test.isValid()) {
            issues.add("$appName has an invalid OSLC test result!")
        }
        when (test.complianceStatus) {
            OslcComplianceStatus.RED -> issues.add("$appName is not compliant! (Rot)")
            OslcComplianceStatus.YELLOW -> issues.add("$appName is not compliant! (Gelb)")
            OslcComplianceStatus.GREEN -> Unit
        }
        if (test.tool.name !in listOf(Tool.OSLC_MAVEN_PLUGIN_NAME, Tool.OSLC_FNCI_NAME) &&
            test.complianceStatus != OslcComplianceStatus.GREEN
        ) {
            issues.add("We have found one or multiple licenses that are not allowed by default. After talking to legal, you might be permitted to mark them as accepted according to this tutorial: https://lcm.deutschepost.de/confluence1/display/SDM/Open+Source+License+Compliance+Scan")
        }
        if (test.policyProfile != OslcTestResult.PROFILE_PLUGIN) {
            if (isDistribution != (!test.policyProfile.startsWith(OslcTestResult.PROFILE_NON_DISTRIBUTION))) {
                issues.add("$appName has policy profile $isDistribution but TestResult has ${test.policyProfile}!")
            }
        }
    }
    check(issues.isEmpty()) {
        buildString {
            append("OSLC has issues:")
            issues.forEach { appendLine(it) }
        }
    }
}

@Suppress("unused")
enum class OslcComplianceStatus {
    GREEN, YELLOW, RED
}
