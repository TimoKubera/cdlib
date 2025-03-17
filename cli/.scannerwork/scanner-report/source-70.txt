package de.deutschepost.sdm.cdlib.change.sharepoint.model


import com.fasterxml.jackson.annotation.JsonProperty

data class SharepointContextWebInformation(
    @JsonProperty("d")
    val d: D
) {
    data class D(
        @JsonProperty("GetContextWebInformation")
        val getContextWebInformation: GetContextWebInformation
    ) {
        data class GetContextWebInformation(
            @JsonProperty("FormDigestValue")
            val formDigestValue: String,
        )
    }
}

fun SharepointContextWebInformation.getDigest() = d.getContextWebInformation.formDigestValue
