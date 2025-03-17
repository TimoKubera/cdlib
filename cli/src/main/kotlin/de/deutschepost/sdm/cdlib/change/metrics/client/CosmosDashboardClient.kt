package de.deutschepost.sdm.cdlib.change.metrics.client

import com.azure.core.credential.TokenCredential
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.models.CosmosContainerResponse
import com.azure.cosmos.models.CosmosDatabaseResponse
import com.azure.cosmos.models.PartitionKey
import com.azure.identity.ClientSecretCredentialBuilder
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import de.deutschepost.sdm.cdlib.change.metrics.model.Deployment
import de.deutschepost.sdm.cdlib.change.metrics.model.Release
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import de.deutschepost.sdm.cdlib.utils.klogSelfWarn
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import mu.KLogging

abstract class CosmosDashboardClient(
    private val clientId: String,
    private val clientSecret: String,
    databaseName: String
) {
    private val database: CosmosDatabase by lazy {
        val servicePrincipal: TokenCredential = ClientSecretCredentialBuilder()
            .tenantId(MCP_TENANT_ID)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build()

        val cosmosClient = CosmosClientBuilder()
            .endpoint(COSMOSDB_ENDPOINT_PROD)
            .credential(servicePrincipal)
            .gatewayMode()
            .buildClient()
        val cosmosDatabaseResponse: CosmosDatabaseResponse = cosmosClient.createDatabaseIfNotExists(databaseName)
        cosmosClient.getDatabase(cosmosDatabaseResponse.properties.id)
    }

    private val releaseInfoContainer: CosmosContainer by lazy {
        val cosmosContainerResponse: CosmosContainerResponse = database.createContainerIfNotExists(
            "release", "/id"
        )
        database.getContainer(cosmosContainerResponse.properties.id)
    }

    private val reportContainer: CosmosContainer by lazy {
        val cosmosContainerResponse: CosmosContainerResponse = database.createContainerIfNotExists(
            "reports", "/id"
        )
        database.getContainer(cosmosContainerResponse.properties.id)
    }

    private val versionInfoContainer: CosmosContainer by lazy {
        val cosmosContainerResponse: CosmosContainerResponse = database.createContainerIfNotExists(
            "supportedVersions", "/version"
        )
        database.getContainer(cosmosContainerResponse.properties.id)
    }


    fun addDeployment(deployment: Deployment) {

        val jsonTree = defaultObjectMapper.valueToTree<JsonNode>(deployment)
        val itemResponse = reportContainer.createItem(jsonTree)

        logger.info { "Dashboard status code: ${itemResponse.statusCode}" }
    }

    fun addRelease(release: Release) {
        val jsonTree = defaultObjectMapper.valueToTree<JsonNode>(release)
        val itemResponse = releaseInfoContainer.createItem(jsonTree)

        logger.info { "Dashboard status code: ${itemResponse.statusCode}" }
    }

    fun getRelease(releaseId: String): Release {
        val itemResponse = releaseInfoContainer.readItem(releaseId, PartitionKey(releaseId), JsonNode::class.java)
        return defaultObjectMapper.treeToValue(itemResponse.item, Release::class.java)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VersionInfo @JsonCreator constructor(
        @param:JsonProperty("id") val id: String = "",
        @param:JsonProperty("version") val version: String = "",
        @param:JsonProperty("isSupported") val isSupported: Boolean = false,
        @param:JsonProperty("isLatest") val isLatest: Boolean = false
    )

    fun getVersionInfo(version: String): VersionInfo {
        val sanitizedVersion: String = version.substringBefore("-")
        val cosmosdbWarnings = mutableListOf<Throwable>()
        val hierarchicalVersionSequence = generateSequence(sanitizedVersion) {
            if (it.contains('.')) {
                it.substringBeforeLast('.')
            } else {
                null
            }
        }
        val versionInfo = hierarchicalVersionSequence.firstNotNullOfOrNull {
            runCatching {
                versionInfoContainer.readItem(
                    it,
                    PartitionKey(it),
                    VersionInfo::class.java
                )
            }.onFailure {
                cosmosdbWarnings.add(it)
            }.getOrNull()
        }

        if (versionInfo != null) {
            return versionInfo.item
        } else {
            cosmosdbWarnings.forEach {
                it.klogSelfWarn(logger)
            }
            throw RuntimeException("Failed to read versionInfo. Look at the preceding warning(s).")
        }
    }

    companion object : KLogging() {
        const val COSMOSDB_ENDPOINT_PROD = "https://cdlib-dashboard.documents.azure.com"
        const val COSMOSDB_DASHBOARD_TEST = "dashboard-test"
        const val COSMOSDB_DASHBOARD_PROD = "dashboard"

        const val MCP_TENANT_ID = "28e748f8-ed1e-4f6f-bd59-788e78989250"
    }
}

@Singleton
class CosmosDashboardClientTEST(
    @Value("\${dashboard-prod-client-id}") clientId: String,
    @Value("\${dashboard-prod-client-secret}") clientSecret: String
) : CosmosDashboardClient(clientId, clientSecret, COSMOSDB_DASHBOARD_TEST)

// az cosmosdb sql role assignment create --subscription "89437d0f-33c0-4f3d-83ed-9fdb9f67bb66" -a cdlib-dashboard -g ICTO-3339_SDM-PROD -s "/" -p cda36622-09c1-4127-8322-a80622ee33b6 -d "00000000-0000-0000-0000-000000000002"
@Singleton
class CosmosDashboardClientPROD(
    @Value("\${dashboard-prod-client-id}") clientId: String,
    @Value("\${dashboard-prod-client-secret}") clientSecret: String
) : CosmosDashboardClient(clientId, clientSecret, COSMOSDB_DASHBOARD_PROD)
