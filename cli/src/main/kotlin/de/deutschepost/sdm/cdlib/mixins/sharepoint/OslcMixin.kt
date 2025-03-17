package de.deutschepost.sdm.cdlib.mixins.sharepoint

import de.deutschepost.sdm.cdlib.change.changemanagement.model.ItSystem
import de.deutschepost.sdm.cdlib.change.sharepoint.SharepointOnlineOslcRepository
import de.deutschepost.sdm.cdlib.change.sharepoint.model.SharepointOslcListItem
import de.deutschepost.sdm.cdlib.mixins.artifactory.TestResultWithMetadata
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import jakarta.inject.Singleton
import mu.KLogging

@Singleton
class OslcMixin(private val sharepointOnlineOslcRepository: SharepointOnlineOslcRepository) {
    private fun createListItem(
        itSystem: ItSystem,
        reportWithMetadata: TestResultWithMetadata,
        isDistribution: Boolean
    ): SharepointOslcListItem {
        require(reportWithMetadata.testResult is OslcTestResult) {
            "Cannot create OslcListItems from other testResults!"
        }
        return SharepointOslcListItem(
            appName = reportWithMetadata.appName,
            itSystem = itSystem,
            reportUrl = reportWithMetadata.url,
            oslcTestResult = reportWithMetadata.testResult,
            isDistribution = isDistribution
        )
    }

    @Suppress("DuplicatedCode")
    fun addEntries(
        isTest: Boolean,
        itSystem: ItSystem,
        isDistribution: Boolean,
        reports: List<TestResultWithMetadata>
    ): List<String> {
        val listItems = reports.map { createListItem(itSystem, it, isDistribution) }
        return if (isTest) {
            logger.info { "Using the OSLC Sharepoint test list!" }
            listItems.map { sharepointOnlineOslcRepository.addEntryTest(it) }
        } else {
            listItems.map { sharepointOnlineOslcRepository.addEntryProd(it) }
        }
    }

    companion object : KLogging()
}
