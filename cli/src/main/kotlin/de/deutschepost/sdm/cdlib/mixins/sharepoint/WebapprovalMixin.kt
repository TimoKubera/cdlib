package de.deutschepost.sdm.cdlib.mixins.sharepoint

import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.change.metrics.model.Webapproval
import de.deutschepost.sdm.cdlib.change.sharepoint.SharepointClient
import de.deutschepost.sdm.cdlib.change.sharepoint.model.SharepointApprovalsListItem
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.findMixinByType
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import mu.KLogging
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Spec

class WebapprovalMixin {
    //Get the command spec (available parameters) for the Mixee
    @Spec(Spec.Target.MIXEE)
    private lateinit var mixee: CommandSpec

    //Extract the sharepoint client from the mixee.
    private val client: SharepointClient by lazy {
        findMixinByType<SharepointClientMixin>(mixee).client
    }

    @CommandLine.ArgGroup(validate = true, exclusive = false, heading = "", multiplicity = "0..1", order = 3)
    lateinit var webApprovalSection: WebApprovalSection

    class WebApprovalSection {
        @Option(
            names = ["--application-id"],
            required = false,
            description = [
                "ID of your pipeline approval configuration for your application",
            ]
        )
        var applicationIdOption: Int? = null
    }


    private val isTest: Boolean by lazy {
        mixee.findOption("--test").getValue()
    }


    private val applicationId by lazy {
        requireNotNull(webApprovalSection.applicationIdOption) {
            "Application ID is required for webapproval."
        }
    }

    private fun createListItem(reportUrl: String, reports: List<Report>) =
        SharepointApprovalsListItem(
            jobUrl = resolveEnvByName(Names.CDLIB_JOB_URL),
            identifier = resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE),
            reportUrl = reportUrl,
            applicationId = applicationId
        ).addReports(reports)

    fun addEntry(reportUrl: String, reports: List<Report>): Webapproval {
        val listItem = createListItem(reportUrl = reportUrl, reports)
        return if (isTest) {
            logger.info { "Using the iShare test list!" }
            client.addEntryTest(listItem)
        } else {
            client.addEntryProd(listItem)
        }
    }

    fun isConfigurationApproved(): Boolean {
        return client.verifyAndGenerateWebapproval(applicationId, isTest).also { logger.debug { it } } != null
    }

    companion object : KLogging()
}
