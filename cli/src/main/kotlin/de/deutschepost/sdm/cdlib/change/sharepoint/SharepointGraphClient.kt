package de.deutschepost.sdm.cdlib.change.sharepoint

import com.example.GraphSharepointResponse
import com.example.GraphSharepointDTO
import de.deutschepost.sdm.cdlib.change.commonClients.O365Configuration
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.retry.annotation.Retryable

// Get Sharepoint id:
// https://dpdhl.sharepoint.com/teams/OSLCReporting/_api/site/id

// Add write permissions with powershell
// Connect-PnPOnline https://dpdhl.sharepoint.com/teams/OSLCReporting -Interactive
// Grant-PnPAzureADAppSitePermission -AppId "f8330f01-0eaf-445c-a427-e69c91b5699e" -DisplayName 'CDlib cli' -Site  https://dpdhl.sharepoint.com/teams/OSLCReporting -Permissions Write

@Client(SHAREPOINT_GRAPH_URL, configuration = O365Configuration::class)
@Retryable
interface SharepointGraphClient {
    @Post("/{siteId}/lists/{listId}/items")
    fun addEntry(
        siteId: String,
        listId: String,
        @Client(SHAREPOINT_GRAPH_URL, configuration = O365Configuration::class)
        @Retryable
        interface SharepointGraphClient {
            @Post("/{siteId}/lists/{listId}/items")
            fun addEntry(
                siteId: String,
                listId: String,
                @Header authorization: String,  // Renamed from Authorization to authorization
                @Body body: GraphSharepointDTO
            ): GraphSharepointResponse
        }
        @Body body: GraphSharepointDTO
    ): GraphSharepointResponse
}

const val SHAREPOINT_GRAPH_URL = "https://graph.microsoft.com/v1.0/sites"


data class GraphSharepointDTO(val fields: Any)

data class GraphSharepointResponse(val id: String, val webUrl: String) {
    val url = buildString {
        append(webUrl.substringBeforeLast("/"))
        append("/DispForm.aspx?ID=")
        append(id)
    }
}
