package de.deutschepost.sdm.cdlib.artifactory

import mu.KotlinLogging
import picocli.CommandLine.Option

const val ARTIFACTORY_URL = "https://lcm.deutschepost.de"
const val AZURE_ARTIFACTORY_URL = "https://artifactory1.lcm.deutschepost.de"
const val ITS_ARTIFACTORY_URL = "https://artifactory.dhl.com"

const val ARTIFACTORY_GUI = "/ui/repos/tree/General"
const val ARTIFACTORY_API = "/artifactory"
const val ARTIFACTORY_ARCHIVE_TYPE_PROPERTY = "archiveType"

class ArtifactoryInstanceSection {
    private val logger = KotlinLogging.logger {}

    @Option(
        names = ["--artifactory-azure-instance"],
        description = ["If this is set the new LCM artifactory instance hosted in Azure is used instead of the legacy one in TCB."]
    )
    var artifactoryAzureInstance: Boolean = false

    @Option(
        names = ["--artifactory-its-instance"],
        description = ["If this is set the IT-S artifactory instance."]
    )
    var artifactoryITSInstance: Boolean = false

    fun selectArtifactoryUrl(): String = when {
        this.artifactoryITSInstance -> {
            ITS_ARTIFACTORY_URL
        }

        this.artifactoryAzureInstance -> {
            AZURE_ARTIFACTORY_URL
        }

        else -> {
            ARTIFACTORY_URL
        }
    }.also { logger.info { "Using Artifactory URL: $it" } }
}
