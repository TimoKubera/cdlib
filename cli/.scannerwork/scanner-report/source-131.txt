package de.deutschepost.sdm.cdlib

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import de.deutschepost.sdm.cdlib.change.metrics.model.CdlibVersionConfig
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import io.micronaut.context.annotation.Context
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import picocli.CommandLine.Option

abstract class SubcommandWithHelp {
    @Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var usageHelpRequested = false

    @Option(names = ["--debug"], description = ["Elevates log level to debug."])
    var debug = false

    @Option(names = ["--trace"], description = ["Elevates log level to trace."])
    var trace = false


    protected fun enableDebugIfOptionIsSet() {
        if (debug) {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            loggerContext.getLogger("de.deutschepost.sdm").level = Level.DEBUG
        }
        if (trace) {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            loggerContext.getLogger("io.micronaut.http").level = Level.TRACE
        }
        logger.info {
            "Starting CDlib ${versionInfo.cdlibVersion}"
        }

    }

    companion object {
        val logger = KotlinLogging.logger {}
        fun printVersionInfo() {
            logger.info {
                "VersionInfo: ${defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(versionInfo)}"
            }
        }
    }
}

private lateinit var versionInfo: CdlibVersionConfig

@Context
class VersionInfoContextInitializer(cdlibVersionConfig: CdlibVersionConfig) {
    init {
        versionInfo = cdlibVersionConfig
    }
}
