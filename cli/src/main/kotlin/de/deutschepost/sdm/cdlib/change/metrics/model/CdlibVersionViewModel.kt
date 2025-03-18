package de.deutschepost.sdm.cdlib.change.metrics.model

import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionConfig
data class CdlibVersionViewModel(
    val cdlib: String,
    val cliReleaseVersion: String,
    val cliContainerTag: String,
    package de.deutschepost.sdm.cdlib.change.metrics.model
    
    data class CdlibVersionViewModel(
        val cdlib: String,
        val cliReleaseVersion: String,
        val cliContainerTag: String,
        val supported: Boolean // Changed from Boolean? to Boolean
    ) {
        constructor(cdlibVersionConfig: CdlibVersionConfig, supported: Boolean) : this(
            cdlib = cdlibVersionConfig.cdlibVersion,
            cliReleaseVersion = cdlibVersionConfig.cliReleaseVersion,
            cliContainerTag = cdlibVersionConfig.cliContainerTag,
            supported = supported
        )
    }
) {
    constructor(cdlibVersionConfig: CdlibVersionConfig, supported: Boolean) : this(
        cdlib = cdlibVersionConfig.cdlibVersion,
        cliReleaseVersion = cdlibVersionConfig.cliReleaseVersion,
        cliContainerTag = cdlibVersionConfig.cliContainerTag,
        supported = supported
    )
}
