package de.deutschepost.sdm.cdlib.release.report

import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.release.report.external.OslcGradlePluginTestResult
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcComplianceChecker
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcDependencyLicenseEntry
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcLicenseDefinition
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcLicenseEntry
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
class OslcComplianceCheckerTest : FunSpec() {

    private val disallowedLicensesDistributionPath: String =
        "src/main/resources/oslc/disallowedLicensesDistribution.json"
    private val disallowedLicensesNonDistributionPath: String =
        "src/main/resources/oslc/disallowedLicensesNonDistribution.json"
    private val reportPath: String = "src/test/resources/oslc/oslc-gradle-plugin-report_failing.json"


    private val depWithMozilla = OslcDependencyLicenseEntry(
        dependencyName = "test.should.fail:mozilla",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "MPL 2.0",
                url = "https://www.mozilla.org/en-US/MPL/2.0/",
            )
        )
    )
    private val licenseMozillaName = "Mozilla Public License 2.0"
    private val depWithCDDL = OslcDependencyLicenseEntry(
        dependencyName = "test.should.fail:cddl",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
                url = "https://oss.oracle.com/licenses/CDDL",
            )
        )
    )
    private val licenseCDDLName = "Common Development and Distribution License 1.0"
    private val depWithLGPL21 = OslcDependencyLicenseEntry(
        dependencyName = "test.should.fail:lgpl21",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1",
                url = "https://www.gnu.org/licenses/lgpl-2.1",
            )
        )
    )
    private val licenseLGPL21Name = "GNU Lesser General Public License 2.1"
    private val depWithLGPL3 = OslcDependencyLicenseEntry(
        dependencyName = "test.should.fail:lgpl3",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "GNU LESSER GENERAL PUBLIC LICENSE, Version 3.0",
                url = "https://www.gnu.org/licenses/lgpl-3.0",
            )
        )
    )
    private val licenseLGPL3Name = "GNU Lesser General Public License 3.0"

    private val depWithGPLCE = OslcDependencyLicenseEntry(
        dependencyName = "test.should.succeed:GPL_CE",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception",
                url = "https://openjdk.java.net/legal/gplv2+ce.html",
            )
        )
    )
    private val licenseCPLCEName = "GNU GPL with Classpath/Linking Exception"
    private val depWithAPL2 = OslcDependencyLicenseEntry(
        dependencyName = "test.should.succeed:APL",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "Apache License 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0",
            )
        )
    )
    private val licenseAPL2Name = null
    private val depWithAladin = OslcDependencyLicenseEntry(
        dependencyName = "test.should.fail:Aladin",
        dependencyVersion = "",
        dependencySources = listOf("my.fantasy.aw"),
        licenses = listOf(
            OslcLicenseEntry(
                license = "Aladin Free Public License",
                url = "http://www.artifex.com/downloads/doc/Public.htm",
            )
        )
    )
    private val licenseAlladinName = "Alladin Free Public License 9"


    init {
        context("findMatchingLicense Tests") {
            test("Table Test for selected Licenses with distribution") {
                io.kotest.data.forAll(
                    row(depWithMozilla, licenseMozillaName),
                    row(depWithCDDL, licenseCDDLName),
                    row(depWithLGPL21, licenseLGPL21Name),
                    row(depWithLGPL3, licenseLGPL3Name),
                    row(depWithGPLCE, null),
                    row(depWithAPL2, null),
                    row(depWithAladin, licenseAlladinName),
                ) { dep: OslcDependencyLicenseEntry, licenseName: String? ->

                    val output = OslcComplianceChecker.findAllMatchingLicenses(dep, true)
                    println("Given: ${dep.licenses.joinToString { "[${it.license} - ${it.url}]" }}")
                    println("Got: ${output.joinToString { "[${it.license}]" }}")

                    if (licenseName != null) {
                        output.isNotEmpty() shouldBe true
                        output.any { it.license == licenseName } shouldBe true
                    } else {
                        output.isEmpty() shouldBe true
                    }

                }
            }

            test("Table Test for selected Licenses with non-distribution") {
                io.kotest.data.forAll(
                    row(depWithMozilla, null),
                    row(depWithCDDL, null),
                    row(depWithLGPL21, null),
                    row(depWithLGPL3, null),
                    row(depWithGPLCE, null),
                    row(depWithAPL2, null),
                    row(depWithAladin, licenseAlladinName),
                ) { dep: OslcDependencyLicenseEntry, licenseName: String? ->
                    val output = OslcComplianceChecker.findAllMatchingLicenses(dep, false)
                    println("Given: ${dep.licenses.joinToString { "[${it.license} - ${it.url}]" }}")
                    println("Got: ${output.joinToString { "[${it.license}]" }}")

                    if (licenseName != null) {
                        output.isNotEmpty() shouldBe true
                        output.any { it.license == licenseName } shouldBe true
                    } else {
                        output.isEmpty() shouldBe true
                    }

                }
            }
        }
        context("Check Reading bundleFile and disallowedLicenses file") {
            test("Distribution File") {
                val definitions = OslcComplianceChecker.buildDefinitions(disallowedLicensesDistributionPath)

                val expectedLicenses = mapOf(
                    "Alladin Free Public License 9" to 5,
                    "Academic Free License 3.0" to 3,
                    "Common Development and Distribution License 1.0" to 6,
                    "Common Development and Distribution License 1.1" to 7,
                    "Eclipse Public License 2.0" to 8,
                    "Eclipse Public License 1.0" to 5,
                    "GNU Lesser General Public License 3.0" to 6,
                    "GNU Library General Public License 2.0" to 3,
                    "GNU Lesser General Public License 2.1" to 9,
                    "GNU General Public License 2.0" to 4,
                    "GNU General Public License 3.0" to 4,
                    "Mozilla Public License 2.0" to 5
                )
                val notExpectedLicenses = listOf(
                    "Apache License 2.0",
                    "Academic Free License 2.0",
                    "BSD 2-clause \"Simplified\" or \"FreeBSD\" License",
                    "BSD License (3-clause, New or Revised) ",
                    "BSD original (4-clause)"
                )
                printDefinitions(definitions)
                expectedLicenses.forEach { (license: String, numberOfAliases: Int) ->
                    val foundDefinition = definitions.firstOrNull { it.license.equals(license, true) }
                    foundDefinition shouldNotBe null
                    if (foundDefinition != null) {
                        (foundDefinition.nameAliases.size + foundDefinition.urlAliases.size) shouldBe numberOfAliases
                    }
                }
                notExpectedLicenses.forEach { license: String ->
                    val foundDefinition = definitions.firstOrNull { it.license.equals(license, true) }
                    foundDefinition shouldBe null
                }
                definitions.forEach {
                    println("License: ${it.license}")
                    it.nameAliases.forEach {
                        println("  -  Name alias: $it")
                    }
                    it.urlAliases.forEach {
                        println("  -  URL alias: $it")
                    }
                }
            }

            test("Non-Distribution File") {
                val definitions = OslcComplianceChecker.buildDefinitions(disallowedLicensesNonDistributionPath)

                val expectedLicenses = mapOf(
                    "Alladin Free Public License 9" to 5,
                    "Academic Free License 3.0" to 3,
                )
                val notExpectedLicenses = listOf(
                    "Apache License 2.0",
                    "Academic Free License 2.0",
                    "BSD 2-clause \"Simplified\" or \"FreeBSD\" License",
                    "BSD License (3-clause, New or Revised) ",
                    "BSD original (4-clause)",
                    "Common Development and Distribution License 1.0",
                    "Common Development and Distribution License 1.1",
                    "Eclipse Public License 2.0",
                    "Eclipse Public License 1.0",
                    "GNU Lesser General Public License 3.0",
                    "GNU Lesser General Public License 2.0",
                    "GNU Lesser General Public License 2.1",
                    "GNU General Public License 2.0",
                    "GNU General Public License 3.0",
                    "Mozilla Public License 2.0"
                )
                printDefinitions(definitions)
                expectedLicenses.forEach { (license: String, numberOfAliases: Int) ->
                    val foundDefinition = definitions.firstOrNull { it.license.equals(license, true) }
                    foundDefinition shouldNotBe null
                    if (foundDefinition != null) {
                        numberOfAliases shouldBe (foundDefinition.nameAliases.size + foundDefinition.urlAliases.size)
                    }
                }
                notExpectedLicenses.forEach { license: String ->
                    val foundDefinition = definitions.firstOrNull { it.license.equals(license, true) }
                    foundDefinition shouldBe null
                }
            }
        }
        context("CheckLicenses Tests") {
            val oslcGradlePluginTestResult: OslcGradlePluginTestResult =
                defaultObjectMapper.readValue(File(reportPath))
            val dependencyLicenseEntries =
                oslcGradlePluginTestResult.dependencies.map { OslcDependencyLicenseEntry.from(it) }
            dependencyLicenseEntries.forEach { entry ->
                println("entry: ${entry.dependencyName} - ${entry.licenses.joinToString { "${it.license}|${it.url}" }}")
            }

            test("With Distribution") {
                val output = OslcComplianceChecker.getIncomliantLicenses(dependencyLicenseEntries, true)
                output.forEach { mapEntry ->
                    println("Found disallowed entry for license: ${mapEntry.key.license} got modules: [${mapEntry.value.joinToString { it.dependencyName }}]")
                }
                output.size shouldBe 7
                val cddl =
                    output.entries.firstOrNull { it.key.license == "Common Development and Distribution License 1.0" }
                cddl shouldNotBe null
                cddl?.value?.size shouldBe 1
                val lgpl21 = output.entries.firstOrNull { it.key.license == "GNU Lesser General Public License 2.1" }
                lgpl21 shouldNotBe null
                lgpl21?.value?.size shouldBe 2
                val gpl21 = output.entries.firstOrNull { it.key.license == "GNU General Public License 2.0" }
                gpl21 shouldNotBe null
                gpl21?.value?.size shouldBe 1
                val epl1 = output.entries.firstOrNull { it.key.license == "Eclipse Public License 1.0" }
                epl1 shouldNotBe null
                epl1?.value?.size shouldBe 2
                val epl2 = output.entries.firstOrNull { it.key.license == "Eclipse Public License 2.0" }
                epl2 shouldNotBe null
                epl2?.value?.size shouldBe 1
                val mpl2 = output.entries.firstOrNull { it.key.license == "Mozilla Public License 2.0" }
                mpl2 shouldNotBe null
                mpl2?.value?.size shouldBe 1
                val unknown = output.entries.firstOrNull { it.key.license == "UNKNOWN" }
                unknown shouldNotBe null
                unknown?.value?.size shouldBe 1

            }
            test("With Non-Distribution") {
                val output = OslcComplianceChecker.getIncomliantLicenses(dependencyLicenseEntries, false)
                output.forEach { mapEntry ->
                    println("Found disallowed entry for license: ${mapEntry.key.license} got modules: [${mapEntry.value.joinToString { "${it.dependencyName}," }}]")
                }
                output.size shouldBe 1
                val unknown = output.entries.firstOrNull { it.key.license == "UNKNOWN" }
                unknown shouldNotBe null
                unknown?.value?.size shouldBe 1

            }
            test("Get All Licenses Test") {
                val output = OslcComplianceChecker.getAllLicensesNames(dependencyLicenseEntries, false)
                output.size shouldBe 76
                output.contains("NULL") shouldBe false

            }
        }
    }
}

private fun printDefinitions(definitions: List<OslcLicenseDefinition>) {
    definitions.forEach {
        println("License: ${it.license}")
        it.nameAliases.forEach {
            println("  -  Name alias: $it")
        }
        it.urlAliases.forEach {
            println("  -  URL alias: $it")
        }
    }
}
