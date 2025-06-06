package de.deutschepost.sdm.cdlib.release.report

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.release.report.external.*
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.SecurityTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.TestResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import mu.KLogging
import java.io.File

object TestResultParser : KLogging() {
    private val defaultPrefixes = TestResultPrefixes()

    fun parse(
        file: File,
        prefixes: TestResultPrefixes = defaultPrefixes,
        substitutes: List<String> = emptyList(),
    ): TestResult? {
        val name = file.name
        logger.debug { "Using overrides: $substitutes" }
        logger.debug { "Current name: $name" }
        try {
            return with(name) {
                when {
                    startsWith(prefixes.fortify) and (endsWith(".fpr")) -> {
                        SecurityTestResult.from(
                            FortifyTestResult.parseFortifyArchive(file.inputStream()),
                            name
                        )
                    }

                    startsWith(prefixes.zap) and endsWith(".json") -> {
                        SecurityTestResult.from(
                            permissiveObjectMapper.readValue(
                                file,
                                ZapTestResult::class.java
                            ), file.name
                        )
                    }

                    startsWith(prefixes.odc) and endsWith(".json") -> {
                        SecurityTestResult.from(
                            permissiveObjectMapper.readValue(
                                file,
                                OdcTestResult::class.java
                            ), file.name
                        )
                    }

                    startsWith(prefixes.cca) and endsWith(".json") -> {
                        runCatching {
                            permissiveObjectMapper.readValue(
                                file,
                                SecurityTestResult::class.java
                            ).copy(uri = file.name, pregenerated = true)
                        }.onFailure {
                            logger.error {
                                """Failed to parse $this.
                                |Are you trying to parse a local Trivy report instead of a CSS Harbor Trivy CCA report?
                                |Only CCA reports generated by cdlib report fetch css are supported""".trimMargin()
                            }

                        }.getOrThrow()
                    }

                    startsWith(prefixes.fnci) and endsWith(".json") -> {
                        val fnciTestResult: FnciTestResult = permissiveObjectMapper.readValue(file)
                        OslcTestResult.from(
                            fnciTestResult,
                            file.name
                        )
                    }

                    startsWith(prefixes.oslcMavenPlugin) and endsWith(".json") -> {
                        val oslcMavenPluginTestResult: OslcMavenPluginTestResult =
                            permissiveObjectMapper.readValue(file)
                        OslcTestResult.from(
                            oslcMavenPluginTestResult,
                            file.name
                        )
                    }

                    startsWith(prefixes.oslcGradlePlugin) and endsWith(".json") -> {
                        val oslcGradlePluginTestResult: OslcGradlePluginTestResult =
                            permissiveObjectMapper.readValue(file)
                        OslcTestPreResult.from(
                            oslcGradlePluginTestResult,
                            file.name
                        )
                    }

                    startsWith(prefixes.oslcNPMPlugin) and endsWith(".json") -> {
                        val oslcNPMPluginTestResult: OslcNPMPluginTestResult =
                            permissiveObjectMapper.readValue(file)
                        OslcTestPreResult.from(
                            oslcNPMPluginTestResult,
                            file.name
                        )
                    }

                    (this in substitutes) and endsWith(".json") -> {
                        permissiveObjectMapper.readValue(
                            file,
                            SecurityTestResult::class.java //TODO Deprecated with CDlib7???
                        ).copy(uri = file.name, pregenerated = true)
                    }

                    else -> {
                        null
                    }
                }.also { testResult ->
                    if (testResult != null) {
                        logger.info { "[${testResult.reportType}] Parsed file: $name - ${testResult.tool.name} - ${testResult.date}" }
                        logger.info { "[${testResult.reportType}]   at ${testResult.uri}" }
                    } else {
                        logger.info { "[-] Skipping file: $name" }
                    }
                }
            }
        } catch (jsonE: JsonParseException) {
            logger.error { "$name was supposed to be parsed but seems to be malformed." }
            throw jsonE
        }
    }
}

data class TestResultPrefixes(
    val zap: String = DEFAULT_PREFIX_ZAP,
    val odc: String = DEFAULT_PREFIX_ODC,
    val fortify: String = DEFAULT_PREFIX_FORTIFY,
    val cca: String = DEFAULT_PREFIX_CCA,
    val fnci: String = DEFAULT_PREFIX_FNCI,
    val oslcMavenPlugin: String = DEFAULT_PREFIX_OSLC_MAVEN_PLUGIN,
    val oslcGradlePlugin: String = DEFAULT_PREFIX_OSLC_GRADLE_PLUGIN,
    val oslcNPMPlugin: String = DEFAULT_PREFIX_OSLC_NPM_PLUGIN,
) {
    companion object {
        const val DEFAULT_PREFIX_ZAP = "zap"
        const val DEFAULT_PREFIX_ODC = "dependency-check"
        const val DEFAULT_PREFIX_FORTIFY = "fortify"
        const val DEFAULT_PREFIX_CCA = "cca"
        const val DEFAULT_PREFIX_FNCI = "oslc-fnci-report"
        const val DEFAULT_PREFIX_OSLC_MAVEN_PLUGIN = "oslc-maven-plugin-report"
        const val DEFAULT_PREFIX_OSLC_GRADLE_PLUGIN = "oslc-gradle-plugin-report"
        const val DEFAULT_PREFIX_OSLC_NPM_PLUGIN = "oslc-npm-plugin-report"
    }
}
