package de.deutschepost.sdm.cdlib.change.metrics.model

import io.micronaut.context.annotation.Property
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
@Suppress("UNUSED")
class CdlibVersionConfig {
    @set:Inject
    @setparam:Property(name = "cdlib-version")
    lateinit var cdlibVersion: String

    @set:Inject
    @setparam:Property(name = "cli-release-version")
    lateinit var cliReleaseVersion: String

    @set:Inject
    @setparam:Property(name = "cli-container-tag")
    lateinit var cliContainerTag: String
}
