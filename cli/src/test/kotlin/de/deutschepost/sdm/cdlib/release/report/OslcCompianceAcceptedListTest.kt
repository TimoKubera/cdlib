package de.deutschepost.sdm.cdlib.release.report

import de.deutschepost.sdm.cdlib.release.report.external.OslcGradlePluginTestResult
import de.deutschepost.sdm.cdlib.release.report.external.from
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcComplianceChecker
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.VersionSpecification
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.from
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import withStandardOutput
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
class OslcComplianceAcceptedListTest : FunSpec() {

    companion object {
        const val INPUT_STRING = "Input String"
    }

    private val acceptedListFileWrongLicense = File("src/test/resources/oslc/acceptedList-wrongLicense.json")
    private val acceptedListFileWrongVersion = File("src/test/resources/oslc/acceptedList-wrongVersion.json")

    private val acceptedListFileEdgeCases = File("src/test/resources/oslc/acceptedList-edge-cases.json")
    private val oslcResultFileEdgeCases = File("src/test/resources/oslc/oslc-gradle-plugin-report_edge-cases.json")

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides(
            mapOf(
                "CDLIB_APP_NAME" to "cli",
                "CDLIB_RELEASE_NAME" to "Integration_result_0228"
            )
        )
    )

    init {
        context("de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.VersionSpecification parsing") {
            test("Testing valid input strings") {
                io.kotest.data.forAll(
                    table(
                        headers(INPUT_STRING, "Default Value", "Expected"),
                        row("0.0.0", 0, VersionSpecification(0, 0, 0)),
                        row("0.0.0", Int.MAX_VALUE, VersionSpecification(0, 0, 0)),
                        row("1.2.3", 0, VersionSpecification(1, 2, 3)),
                        row("1.2.3.4", 0, VersionSpecification(1, 2, 3)),
                        row("1", 0, VersionSpecification(1, 0, 0)),
                        row("6.6", 6, VersionSpecification(6, 6, 6)),
                    )
                ) { input: String, default: Int, expected: VersionSpecification ->
                    VersionSpecification.fromString(input, default) shouldBe expected
                }
            }

            test("Testing invalid input strings") {
                io.kotest.data.forAll(
                    table(
                        headers(INPUT_STRING, "Default Value"),
                        row("a.b.c", 0),
                        row("error", 0),
                        row("3,2,5", 0),
                    )
                ) { input: String, default: Int ->
                    shouldThrow<NumberFormatException> { VersionSpecification.fromString(input, default) }
                }
            }

            test("Testing valid ranged input strings") {
                io.kotest.data.forAll(
                    table(
                        headers(INPUT_STRING, "ExpectedMin", "ExpectedMax"),
                        row("1.2.3-11.12.13", VersionSpecification(1, 2, 3), VersionSpecification(11, 12, 13)),
                        row("-11.12.13", VersionSpecification(0, 0, 0), VersionSpecification(11, 12, 13)),
                        row(
                            "1.2.3-",
                            VersionSpecification(1, 2, 3),
                            VersionSpecification(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
                        ),
                        row("1-1.5", VersionSpecification(1, 0, 0), VersionSpecification(1, 5, Int.MAX_VALUE)),
                        row(
                            "-",
                            VersionSpecification(0, 0, 0),
                            VersionSpecification(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
                        ),
                        row(
                            "",
                            VersionSpecification(0, 0, 0),
                            VersionSpecification(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
                        ),
                        row("11.12.13", VersionSpecification(11, 12, 13), VersionSpecification(11, 12, 13)),
                    )
                ) { input: String, min: VersionSpecification, max: VersionSpecification ->
                    VersionSpecification.rangeFromString(input) shouldBe min..max
                }
            }

            test("Testing invalid ranged input strings") {
                io.kotest.data.forAll(
                    table(
                        headers(INPUT_STRING),
                        row("1.2.3-11.12.13-1.2.3"),
                        row("1-1-1"),
                    )
                ) { input: String ->
                    shouldThrow<RuntimeException> { VersionSpecification.rangeFromString(input) }
                }
            }

            test("Testing isInBetween") {
                io.kotest.data.forAll(
                    table(
                        headers("Min", "Input", "Max", "Expected"),
                        row(
                            VersionSpecification(1, 2, 3),
                            VersionSpecification(1, 3, 3),
                            VersionSpecification(2, 1, 1),
                            true
                        ),
                        row(
                            VersionSpecification(1, 2, 3),
                            VersionSpecification(1, 1, 3),
                            VersionSpecification(2, 1, 1),
                            false
                        ),
                        row(
                            VersionSpecification(1, 2, 3),
                            VersionSpecification(1, 2, 3),
                            VersionSpecification(1, 2, 3),
                            true
                        ),
                        row(
                            VersionSpecification(1, 0, 0),
                            VersionSpecification(3, 0, 0),
                            VersionSpecification(2, 0, 0),
                            false
                        ),
                        row(
                            VersionSpecification(1, 0, 0),
                            VersionSpecification(2, 0, 1),
                            VersionSpecification(2, 1, 0),
                            true
                        ),

                        )
                ) { min: VersionSpecification, input: VersionSpecification, max: VersionSpecification, expected: Boolean ->
                    (input in min..max) shouldBe expected
                }

            }
        }

        context("Testing Accept List Parsing") {
            test("Parsing List with incompliant License") {
                val list = OslcComplianceChecker.readAcceptList(acceptedListFileWrongLicense)

                list.entries.size shouldBe 5

                list.entries.forEach {
                    println("Entry: $it")
                    if (it.packageName == "com.rabbitmq:amqp-client") {
                        (it.newLicense?.license ?: "") shouldBe "ALLADIN"
                    } else {
                        (it.newLicense?.license ?: "") shouldBe "Apache 2.0"
                    }
                }
            }

            test("Parsing List with version number") {
                val list = OslcComplianceChecker.readAcceptList(acceptedListFileWrongVersion)

                list.entries.size shouldBe 5

                list.entries.forEach {
                    println("Entry: $it")
                    if (it.packageName == "org.apache.tomcat.embed:tomcat-embed-core") {
                        it.version shouldBe "1-1.1"
                    } else {
                        it.version shouldBe ""
                    }
                }
            }
        }

        context("Edge Cases") {
            var result: OslcTestResult? = null
            val (ret, output) = withStandardOutput {
                val preResult = OslcTestPreResult.from(
                    permissiveObjectMapper.readValue(
                        oslcResultFileEdgeCases,
                        OslcGradlePluginTestResult::class.java
                    ), oslcResultFileEdgeCases.name
                )
                result = OslcTestResult.from(preResult, true, acceptedListFileEdgeCases)
            }
            test("Check Output") {
                output shouldContain "OslcComplianceChecker has 3 overwritten licenses"
                output shouldContain "DependencyEntry [org.edge.case:caseOne] was manually approved"
                output shouldContain "DependencyEntry [org.edge.case:caseTwo] was clarified to Apache 2.0"
                output shouldContain "DependencyEntry [org.edge.case:caseThree] was clarified to LPPL"
            }
            test("Check Results") {
                result?.unapprovedItems?.keys?.size shouldBe 3
                result?.unapprovedItems?.keys?.shouldContain("Alladin Free Public License 9")
                result?.unapprovedItems?.keys?.shouldContain("LaTeX Project Public License")
                result?.unapprovedItems?.keys?.shouldContain("UNKNOWN")
                result?.unapprovedItems?.get("Alladin Free Public License 9")?.get("REJECTED")
                    ?.shouldContain("org.edge.case:caseBase")
                result?.unapprovedItems?.get("LaTeX Project Public License")?.get("REJECTED")
                    ?.shouldContain("org.edge.case:caseThree")
                result?.unapprovedItems?.get("UNKNOWN")?.get("REJECTED")?.shouldContain("org.edge.case:caseFive")
            }
        }
    }
}
