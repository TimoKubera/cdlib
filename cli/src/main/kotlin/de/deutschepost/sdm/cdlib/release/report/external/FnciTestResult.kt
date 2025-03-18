package de.deutschepost.sdm.cdlib.release.report.external

import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciInventoryItem
import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfo
import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.Tool

data class FnciTestResult(
    val fnciProjectInfo: FnciProjectInfo, val fnciProjectInventory: List<FnciInventoryItem>
)

fun OslcTestResult.Companion.from(fnciTestResult: FnciTestResult, uri: String): OslcTestResult {
    val fnciInventoryUnapproved =
        fnciTestResult.fnciProjectInventory
            .filter { item -> item.inventoryReviewStatus != "Approved" }
            .groupBy { it.selectedLicenseName }
            .mapValues { groupedMapEntry ->
                groupedMapEntry.value.groupBy({ item -> item.inventoryReviewStatus }, { item -> item.name })
            }
    return OslcTestResult(
        ```kotlin
        package de.deutschepost.sdm.cdlib.release.report.external
        
        import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciInventoryItem
        import de.deutschepost.sdm.cdlib.release.report.external.fnci.FnciProjectInfo
        import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
        import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
        import de.deutschepost.sdm.cdlib.release.report.internal.Tool
        
        data class FnciTestResult(
            val fnciProjectInfo: FnciProjectInfo, val fnciProjectInventory: List<FnciInventoryItem>
        )
        
        fun OslcTestResult.Companion.from(fnciTestResult: FnciTestResult, uri: String): OslcTestResult {
            val fnciInventoryUnapproved =
                fnciTestResult.fnciProjectInventory
                    .filter { item -> item.inventoryReviewStatus != "Approved" }
                    .groupBy { it.selectedLicenseName }
                    .mapValues { groupedMapEntry ->
                        groupedMapEntry.value.groupBy({ item -> item.inventoryReviewStatus }, { item -> item.name })
                    }
            return OslcTestResult(
                tool = Tool(
                    name = "Corrected Tool Name", version = "1.0", vendor = "Corrected Vendor", ruleVersion = "1.0"
                ),
                uri = uri,
                uniqueLicenses = fnciTestResult.fnciProjectInventory.map { item ->
                    item.selectedLicenseName
                }.distinct().sorted(),
                projectName = fnciTestResult.fnciProjectInfo.name,
                projectId = fnciTestResult.fnciProjectInfo.id,
                depth = "libs", //FNCI cannot create libs+code
                policyProfile = fnciTestResult.fnciProjectInfo.policyProfileName,
                unapprovedItems = fnciInventoryUnapproved,
                totalArtifactCount = fnciTestResult.fnciProjectInventory.size,
                complianceStatus = when {
                    fnciInventoryUnapproved.isEmpty() -> OslcComplianceStatus.GREEN
                    else -> OslcComplianceStatus.RED
                }
            )
        }
        ```
            name = Tool.OSLC_FNCI_NAME, version = "", vendor = "Revenera", ruleVersion = ""
        ),
        uri = uri,
        uniqueLicenses = fnciTestResult.fnciProjectInventory.map { item ->
            item.selectedLicenseName
        }.distinct().sorted(),
        projectName = fnciTestResult.fnciProjectInfo.name,
        projectId = fnciTestResult.fnciProjectInfo.id,
        depth = "libs", //FNCI cannot create libs+code
        policyProfile = fnciTestResult.fnciProjectInfo.policyProfileName,
        unapprovedItems = fnciInventoryUnapproved,
        totalArtifactCount = fnciTestResult.fnciProjectInventory.size,
        complianceStatus = when {
            fnciInventoryUnapproved.isEmpty() -> OslcComplianceStatus.GREEN
            else -> OslcComplianceStatus.RED
        }
    )
}
