package de.deutschepost.sdm.cdlib.change.sharepoint.model

sealed interface SharepointListItem {
    val listNameProd: String
    val listNameTest: String
}
