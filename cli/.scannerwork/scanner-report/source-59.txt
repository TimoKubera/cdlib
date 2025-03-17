package de.deutschepost.sdm.cdlib.change.metrics.model

data class ChangeUrlFile(
    val cdlibChangeJiraUrl: String,
    val cdlibChangeImmutableRepoUrl: String?,
    val cdlibChangeWebapprovalEntryUrl: String?,
    val cdlibChangeOslcEntryUrls: List<String>,
    val cdlibChangeTqsEntryUrls: List<String>,
)
