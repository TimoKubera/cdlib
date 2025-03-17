package de.deutschepost.sdm.cdlib.change.sharepoint.model


import com.fasterxml.jackson.annotation.JsonProperty

data class SharepointApprovalConfigurations(
    @JsonProperty("d")
    val d: D = D()
) {
    data class D(
        @JsonProperty("results")
        val items: List<SharepointApprovalConfiguration> = listOf()
    ) {
        data class SharepointApprovalConfiguration(
            @JsonProperty("Approval_x0020_Status")
            val status: String?,
            @JsonProperty("Id")
            val listId: Int,
        )
    }

    fun findById(id: Int): D.SharepointApprovalConfiguration? = d.items.find { it.listId == id }
}
