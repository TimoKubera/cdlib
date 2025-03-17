package de.deutschepost.sdm.cdlib.release.report.external.cca


import com.fasterxml.jackson.annotation.JsonProperty

class CcaSuppressionList : ArrayList<CcaSuppressionList.CcaSuppressionListItem>() {
    data class CcaSuppressionListItem(
        @JsonProperty("cve_allowlist")
        val cveAllowlist: CveAllowlist,
    ) {
        data class CveAllowlist(
            @JsonProperty("items")
            val items: List<Item>,
        ) {
            data class Item(
                @JsonProperty("cve_id")
                val cveId: String?
            )
        }
    }

    fun getCveIds(): List<String> =
        this.map { it.cveAllowlist.items.mapNotNull { item -> item.cveId } }.flatten()
}
