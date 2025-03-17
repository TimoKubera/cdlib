package de.deutschepost.sdm.cdlib.change.changemanagement.model

data class CreateChangeResponse(
    val id: String,
    override val key: String,
) : HumanReadableLink
