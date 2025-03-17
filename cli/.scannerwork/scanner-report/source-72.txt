package de.deutschepost.sdm.cdlib.change.sharepoint.model

sealed interface SharepointListItemId {
    fun getUrl(baseUrl: String, listName: String): String
}
