package de.deutschepost.sdm.cdlib.change.sharepoint

import de.deutschepost.sdm.cdlib.change.commonClients.TestConnectionClient
import de.deutschepost.sdm.cdlib.change.sharepoint.model.SharepointListItem
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import jakarta.inject.Named
import jakarta.inject.Singleton
import mu.KLogging


@Singleton
class SharepointOnlineOslcRepository(
    @param:Named("o365") private val testConnectionClient: TestConnectionClient,
    private val sharepointGraphClient: SharepointGraphClient,
    private val graphTokenProvider: GraphTokenProvider,
) {

    fun addEntryProd(listItem: SharepointListItem) =
        addEntry(listItem, listItem.listNameProd)

    fun addEntryTest(listItem: SharepointListItem) =
        addEntry(listItem, listItem.listNameTest)

    private fun addEntry(
        listItem: SharepointListItem,
        listName: String
    ) = runCatching {
        testConnectionClient.testConnection(SHAREPOINT_GRAPH_URL)

        logger.info { "Creating a record in OSLCReporting/Lists/$listName. " }

        logger.debug {
            val jsonEntry = defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listItem)
            "OSLC Sharepoint Entry:\n$jsonEntry"
        }

        val response = sharepointGraphClient.addEntry(
            SHAREPOINT_OSLC_ID,
            listName,
            graphTokenProvider.token,
            GraphSharepointDTO(listItem)
        )
        logger.info {
            "EntryUrl: ${response.url}"
        }

        response.url
    }.onFailure {
        logger.error { "Failed to create OSLC Sharepoint Entry." }
    }.getOrThrow()


    companion object : KLogging() {
        const val SHAREPOINT_OSLC_ID = "58b29c65-a53a-4da0-a890-de4875c0715f"
    }
}
