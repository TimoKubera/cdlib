package de.deutschepost.sdm.cdlib.release.report.external

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import de.deutschepost.sdm.cdlib.release.report.external.fortify.EngineData
import de.deutschepost.sdm.cdlib.release.report.external.fortify.FortifyVulnerability
import de.deutschepost.sdm.cdlib.release.report.internal.*
import de.deutschepost.sdm.cdlib.utils.defaultXmlMapper
import mu.KLogging
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream

data class FortifyTestResult(
    @JacksonXmlProperty(localName = "CreatedTS")
    val createdTimestamp: Timestamp,
    @JacksonXmlProperty(localName = "Build")
    val build: Build,
    @JacksonXmlProperty(localName = "EngineData")
    val engineData: EngineData,

    @JacksonXmlProperty(localName = "Vulnerabilities")
    val fortifyVulnerabilities: List<FortifyVulnerability>
) {
    val ratedVulnerabilities by lazy {
        fortifyVulnerabilities.map { vuln ->
            val rule = engineData.ruleInfo.find { vuln.classInfo.classID == it.id }
            val impact = rule?.impact ?: 0.0
            val likelihood =
                if (rule != null) {
                    rule.accuracy * rule.probability * vuln.confidence / 25
                } else {
                    0.0
                }
            val severity = when {
                (impact >= 2.5) and (likelihood >= 2.5) -> Severity.CRITICAL
                (impact >= 2.5) and (likelihood < 2.5) -> Severity.HIGH
                (impact < 2.5) and (likelihood >= 2.5) -> Severity.MEDIUM
                (impact < 2.5) and (likelihood < 2.5) -> Severity.LOW
                else -> Severity.UNKNOWN
            }
            Vulnerability(
                id = vuln.instanceInfo.instanceID,
                severity = severity,
                description = vuln.classInfo.kingdom,
                origin = vuln.location,
                additionalInfo = mapOf(
                    Pair("snippet", vuln.snippet)
                )
            )
        }
    }

    data class Timestamp(
        @JacksonXmlProperty(isAttribute = true, localName = "date")
        val date: LocalDate,
        @JacksonXmlProperty(isAttribute = true, localName = "time")
        val time: LocalTime
    )

    data class Build(
        @JacksonXmlProperty(localName = "SourceFiles")
        val sourceFiles: List<SourceFile>
    )

    data class SourceFile(
        @JacksonXmlProperty(isAttribute = true, localName = "size")
        val size: Int,
        @JacksonXmlProperty(localName = "Name")
        val name: String
    )

    companion object FPRHandler : KLogging() {
        fun parseFortifyXML(text: String): FortifyTestResult {
            return try {
                defaultXmlMapper.readValue(text, FortifyTestResult::class.java)
            } catch (missingE: MismatchedInputException) {
                logger.error { "[SAST] Unable to parse input. An expected parameter was missing." }
                logger.error { "[SAST] If this problem persists please create a bug report here: https://git.dhl.com/CDLib/CDlib/issues/new?assignees=ab6jg8&labels=%3Abug%3A+bug&template=bug_report.md&title=report%20check%20fortify" }
                throw missingE
            } catch (jsonE: JacksonException) {
                logger.error { "[SAST] Unable to parse input. Please check if audit.fvdl is part of the .fpr file (e.g. with 7-zip)." }
                throw jsonE
            }
        }

        fun parseFortifyArchive(inputStream: InputStream): Pair<FortifyTestResult, List<String>> {
            var report: FortifyTestResult? = null
            val suppressions: MutableList<String> = mutableListOf()
            ZipInputStream(inputStream).use { zipInputStream ->
                generateSequence { zipInputStream.nextEntry }
                    .filter { entry -> entry.name.matches(Regex(".*filters/.*txt|audit.fvdl")) }
                    .mapNotNull { entry ->
                        when {
                            entry.name.endsWith("fvdl") -> parseFortifyXML(
                                zipInputStream.bufferedReader().readText()
                            )

                            entry.name.endsWith(".txt") -> zipInputStream.bufferedReader().readText()
                                .split("\n")
                                .filter { (!it.startsWith("#")) && it.isNotEmpty() }

                            else -> null
                        }
                    }.forEach {
                        @Suppress("UNCHECKED_CAST") //Only ever adds strings
                        when (it) {
                            is FortifyTestResult -> report = it
                            is List<*> -> suppressions.addAll(it as List<String>)
                        }
                    }
            }
            report?.let {
                return it to suppressions.toList()
            }
            throw NoSuchElementException("Couldn't parse audit.fvdl.")
        }
    }
}

fun SecurityTestResult.Companion.from(
    externalFortifyPair: Pair<FortifyTestResult, List<String>>,
    uri: String,
) =
    SecurityTestResult(
        uri = uri,
        date = ZonedDateTime.of(
            externalFortifyPair.first.createdTimestamp.date,
            externalFortifyPair.first.createdTimestamp.time,
            ZoneId.systemDefault()
        ),
        reportType = ReportType.SAST,
        tool = Tool(
            name = "Fortify",
            vendor = "IT-S",
            version = externalFortifyPair.first.engineData.fortifyVersion,
            ruleVersion = externalFortifyPair.first.engineData.fortifyVersion
        ),
        vulnerabilities = Vulnerabilities(
            scannedObjects = externalFortifyPair.first.build.sourceFiles.size,
            vulnerableObjects = externalFortifyPair.first.ratedVulnerabilities.size,
            open = externalFortifyPair.first.ratedVulnerabilities,
            suppressed = externalFortifyPair.second,
        )
    )
