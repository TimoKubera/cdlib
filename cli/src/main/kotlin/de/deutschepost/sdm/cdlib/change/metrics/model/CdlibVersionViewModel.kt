package de.deutschepost.sdm.cdlib.change.metrics.model

data class CdlibVersionViewModel(
    val cdlib: String,
    val cliReleaseVersion: String,
    val cliContainerTag: String,
    val supported: Boolean? //TODO make non nullable with CDlib 7
) {
    constructor(cdlibVersionConfig: CdlibVersionConfig, supported: Boolean) : this(
        cdlib = cdlibVersionConfig.cdlibVersion,
        cliReleaseVersion = cdlibVersionConfig.cliReleaseVersion,
        cliContainerTag = cdlibVersionConfig.cliContainerTag,
        supported = supported
    )
}
