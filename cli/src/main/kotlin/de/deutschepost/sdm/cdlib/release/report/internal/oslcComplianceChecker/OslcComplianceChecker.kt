package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import mu.KLogging
import java.io.File


data class LicenseBlacklist(
    val disallowedLicenses: List<LicenseBlackListEntry>
)

data class LicenseBlackListEntry(
    val name: String,
    val url: String,
    val shortIdentifier: String,
    val version: String
)

object OslcComplianceChecker : KLogging() {

    private const val NOT_FOUND_MESSAGE = "not found"

    private val disallowedLicensesDistributionPath: String =
        javaClass.getResource("/oslc/disallowedLicensesDistribution.json")?.path ?: NOT_FOUND_MESSAGE
    private val disallowedLicensesNonDistributionPath: String =
        javaClass.getResource("/oslc/disallowedLicensesNonDistribution.json")?.path ?: NOT_FOUND_MESSAGE
    private val bundlerPath: String = javaClass.getResource("/oslc/licenseBundler.json")?.path ?: NOT_FOUND_MESSAGE

    private val licenseDefinitionsDistribution: List<OslcLicenseDefinition> by lazy {
        buildDefinitions(
            disallowedLicensesDistributionPath
        )
    }
    private val licenseDefinitionsNonDistribution: List<OslcLicenseDefinition> by lazy {
        buildDefinitions(
            disallowedLicensesNonDistributionPath
        )
    }

    private fun getDefinitions(isDistribution: Boolean): List<OslcLicenseDefinition> {
        return if (isDistribution) licenseDefinitionsDistribution else licenseDefinitionsNonDistribution
    }

    fun readAcceptList(file: File): OslcAcceptList {
        val list: OslcAcceptList = permissiveObjectMapper.readValue(file)
        val violations = list.entries.filter {
            it.justification.isBlank()
        }.map {
            "Please provide a justification for (${it.packageName})"
        }

        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("The given whitelist (${file.path}) contains a overwrite entry without a justification:")
                violations.forEach {
                    appendLine(it)
                }
            }
            throw RuntimeException(message)
        }

        return list
    }


    fun convertOslcPreResultsToOslcResults(
        reports: List<Report>,
        isDistribution: Boolean,
        acceptListFile: File?
    ): List<Report> =
        reports.map { report ->
            if (report.test.reportType == ReportType.OSLC_PRE) {
                report.copy(
                    test = OslcTestResult.from(report.test as OslcTestPreResult, isDistribution, acceptListFile)
                )
            } else {
                report
            }
        }

    fun buildDefinitions(blacklistFilepath: String): List<OslcLicenseDefinition> {
        val bundleData: BundlerData = permissiveObjectMapper.readValue(File(bundlerPath))
        val blacklist: LicenseBlacklist = permissiveObjectMapper.readValue(File(blacklistFilepath))

        val mapFromBundler = blacklist.disallowedLicenses.associateWith { license ->
            // For each blacklisted license, this will search for any TransformationRule that fits the given license`s name, url or bundleName/shortIdentifier
            // And then associate all TransformationRules with the same bundle name with the given licenses
            // The bundle name comes from the bundler file, The licenses infos from the whitelist file
            // When there is no TransformationRule fitting the given license name, the license will be associated with a List containing the given license name and url
            // Example
            // "Apache License 2.0" -> [
            //      { "bundleName" : "Apache-2.0", "licenseNamePattern" : ".*The Apache Software License, Version 2\\.0.*" },
            //      { "bundleName" : "Apache-2.0", "licenseNamePattern" : "ASL 2\\.0" },
            //      { "bundleName" : "Apache-2.0", "licenseUrlPattern" : ".*(www\\.)?opensource\\.org/licenses/Apache-2\\.0.*" },
            // ]
            val parsedTransformationRules = bundleData.transformationRules + bundleData.bundles.flatMap {
                // Also add bundle information itself as transformation rule
                listOf(
                    BundlerDataTransformationRules(
                        bundleName = it.bundleName,
                        licenseNamePattern = it.licenseName.trim()
                    ),
                    BundlerDataTransformationRules(
                        bundleName = it.bundleName,
                        licenseUrlPattern = it.licenseUrl.trim()
                    ),
                )
            }

            val groupedTransformationRules =
                parsedTransformationRules.groupBy {
                    it.bundleName
                }.values.firstOrNull { rules: List<BundlerDataTransformationRules> ->
                    rules.any { doesTransformationRuleApplyToBlackListedLicense(it, license) }
                } ?: listOf(
                    BundlerDataTransformationRules(
                        bundleName = license.shortIdentifier,
                        licenseNamePattern = adjustLicenseNameWithVersion(license.name.trim(), license.version)
                    ),
                    BundlerDataTransformationRules(
                        bundleName = license.shortIdentifier,
                        licenseUrlPattern = license.url
                    )
                )

            val bundlerNameTransformationRule = BundlerDataTransformationRules(
                bundleName = license.shortIdentifier,
                licenseNamePattern = license.shortIdentifier
            )

            groupedTransformationRules + bundlerNameTransformationRule
        }
        val listFromDisallowed =
            mapFromBundler.map { (license: LicenseBlackListEntry, rules: List<BundlerDataTransformationRules>) ->
                // This will create the internal data class OslcLicenseDefinition from the given entries.
                // When no TransformationRule is given, the OslcLicenseDefinition will still be constructed with empty lists,
                // since the plain license name can still produce matching results later on
                if (rules.isNotEmpty()) {
                    OslcLicenseDefinition(
                        license = adjustLicenseNameWithVersion(license.name.trim(), license.version),
                        nameAliases = rules.filter { it.licenseNamePattern.isNotBlank() }.map { it.licenseNamePattern },
                        urlAliases = rules.filter { it.licenseUrlPattern.isNotBlank() }.map { it.licenseUrlPattern },
                        isCompliant = false,
                    )
                } else {
                    OslcLicenseDefinition(license.name.trim(), emptyList(), emptyList(), false)
                }
            }
        return listFromDisallowed.plus(
            OslcLicenseDefinition(
                license = "UNKNOWN",
                nameAliases = listOf("UNKNOWN"),
                urlAliases = emptyList(),
                isCompliant = false
            )
        )
    }

    private fun doesTransformationRuleApplyToBlackListedLicense(
        rule: BundlerDataTransformationRules,
        license: LicenseBlackListEntry
    ): Boolean {
        return (rule.licenseNamePattern.isNotBlank() && rule.licenseNamePattern.containsIgnoreCase(
            adjustLicenseNameWithVersion(license.name.trim(), license.version)
        )) ||
            (rule.licenseUrlPattern.isNotBlank() && license.url.containsIgnoreCase(rule.licenseUrlPattern)) ||
            (rule.bundleName.isNotBlank() && license.shortIdentifier.containsIgnoreCase(rule.bundleName))
    }

    private fun adjustLicenseNameWithVersion(name: String, version: String): String {
        val possibleFloat = version.toFloatOrNull()
        return if (possibleFloat != null && name.isNotEmpty() && !name.last().isDigit()) {
            "$name $version"
        } else {
            name
        }
    }

    fun getIncomliantLicenses(
        entries: List<OslcDependencyLicenseEntry>,
        isDistributionDefinitions: Boolean,
    ): Map<OslcLicenseDefinition, List<OslcDependencyLicenseEntry>> {
        val possibleEvilEntries = entries.filter { entry ->
            if (entry.isOverwritten) {
                if (entry.isManuallyApproved) {
                    logger.info { "DependencyEntry [${entry.dependencyName}] was manually approved" }
                } else {
                    logger.info { "DependencyEntry [${entry.dependencyName}] was clarified to ${entry.licenses.firstOrNull()?.license}" }
                }
            }
            !entry.isManuallyApproved
        }

        return possibleEvilEntries.associateWith { entry ->
            findAllMatchingLicenses(entry, isDistributionDefinitions)
        }.flatMap { mapEntry ->
            mapEntry.value.map { it to mapEntry.key }
        }.groupBy({ it.first }, { it.second })
    }

    fun getAllLicensesNames(
        dependencyEntries: List<OslcDependencyLicenseEntry>,
        isDistribution: Boolean
    ): List<String> =
        dependencyEntries.flatMap { entry ->
            entry.licenses.map {
                findMatchingLicense(it, isDistribution)?.license ?: it.license
            }
        }

    fun findAllMatchingLicenses(
        dependencyEntry: OslcDependencyLicenseEntry,
        isDistributionDefinitions: Boolean
    ): List<OslcLicenseDefinition> =
        dependencyEntry.licenses.mapNotNull {
            findMatchingLicense(it, isDistributionDefinitions)
        }.distinct()

    private fun findMatchingLicense(
        licenseEntry: OslcLicenseEntry,
        isDistributionDefinitions: Boolean
    ): OslcLicenseDefinition? =
        getDefinitions(isDistributionDefinitions).firstOrNull {
            it.isOslcLicenseEntryMatching(licenseEntry)
        }

    fun String.containsIgnoreCase(regexStr: String): Boolean {
        val regex = regexStr.toRegex(RegexOption.IGNORE_CASE)
        return this.contains(regex)
    }

}
