package de.deutschepost.sdm.cdlib.change.metrics.client

import de.deutschepost.sdm.cdlib.change.commonClients.TestConnectionClient
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardClient.Companion.COSMOSDB_ENDPOINT_PROD
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionConfig
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionViewModel
import de.deutschepost.sdm.cdlib.change.metrics.model.Deployment
import de.deutschepost.sdm.cdlib.change.metrics.model.Release
import jakarta.inject.Named
import jakarta.inject.Singleton
import mu.KLogging

@Singleton
class CosmosDashboardRepository(
    private val clientTEST: CosmosDashboardClientTEST,
    private val clientPROD: CosmosDashboardClientPROD,
    private val cdlibVersionConfig: CdlibVersionConfig,
    @param:Named("default") private val testConnectionClient: TestConnectionClient
) {
    val versionInfo: CosmosDashboardClient.VersionInfo by lazy {
        clientPROD.getVersionInfo(cdlibVersionConfig.cdlibVersion)
    }

    fun getCdlibVersionViewModel() = CdlibVersionViewModel(cdlibVersionConfig, versionInfo.isSupported)

    fun addDeployment(deployment: Deployment, isTest: Boolean) = runCatching {
        val client = if (isTest) clientTEST else clientPROD
        client.addDeployment(deployment)
        logger.info { "Published metrics to Azure Devops Dashboard successfully." }
    }.onFailure {
        logger.error { "Failed publishing metrics to Azure Devops Dashboard." }
    }.getOrThrow()

    fun addRelease(release: Release, isTest: Boolean) = runCatching {
        val client = if (isTest) clientTEST else clientPROD
        client.addRelease(release)
        logger.info { "Published Release to Azure CosmosDB successfully." }
    }.onFailure {
        logger.error { "Failed publishing Release to Azure CosmosDB." }
    }.getOrThrow()

    fun getRelease(releaseId: String, isTest: Boolean): Release {
        val client = if (isTest) clientTEST else clientPROD
        return client.getRelease(releaseId)
    }

    fun testConnection(): Boolean {
        return testConnectionClient.testConnection(COSMOSDB_ENDPOINT_PROD)
    }

    companion object : KLogging()
}
