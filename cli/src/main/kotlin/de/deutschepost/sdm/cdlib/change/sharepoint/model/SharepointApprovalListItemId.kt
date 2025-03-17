package de.deutschepost.sdm.cdlib.change.sharepoint.model


import com.fasterxml.jackson.annotation.JsonProperty

data class SharepointApprovalListItemId(
    @JsonProperty("d")
    val d: D
) : SharepointListItemId {
    data class D(
        @JsonProperty("ID")
        val iD: Int,
    )

    override fun getUrl(baseUrl: String, listName: String): String {
        return "$baseUrl/Lists/$listName/DispForm.aspx?ID=${d.iD}"
    }
}
