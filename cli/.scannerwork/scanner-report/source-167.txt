package de.deutschepost.sdm.cdlib.mixins.sharepoint

import com.fasterxml.jackson.module.kotlin.readValue
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.mixins.CheckMixin
import de.deutschepost.sdm.cdlib.release.report.internal.OslcComplianceStatus
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import de.deutschepost.sdm.cdlib.utils.sha256sum
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import java.io.File

@RequiresTag("UnitTest")
@Tags("UnitTest")
class OslcMixinTest : FunSpec() {
    private val checkMixin = CheckMixin()
    private val testResultPath = "src/test/resources/fnci/cdlib_test_OSLC_19C2_ICTO-3339_SDM_SockShop.json"

    override fun listeners() = listOf(
        getSystemEnvironmentTestListenerWithOverrides()
    )

    private val testResultFile = File(testResultPath)
    private val testResult: OslcTestResult = permissiveObjectMapper.readValue(testResultFile)
    val report by lazy { Report(test = testResult, testHash = testResultFile.sha256sum()) }

    init {


        test("OslcMixin should complain") {
            shouldThrow<IllegalStateException> {
                val modifiedReport = report.copy(
                    test = (report.test as OslcTestResult).copy(
                        complianceStatus = OslcComplianceStatus.RED
                    )
                )
                checkMixin.checkOslcCompliance(modifiedReport.name, listOf(modifiedReport), false)
            }
        }

        test("OslcMixin should complain about impossible reports") {
            shouldThrow<IllegalStateException> {
                val modifiedReport = report.copy(
                    test = (report.test as OslcTestResult).copy(
                        unapprovedItems = mapOf("Draft" to mapOf("Minecraft Mod Public License" to listOf("It's on Github!")))
                    )
                )
                checkMixin.checkOslcCompliance(modifiedReport.name, listOf(modifiedReport), false)
            }
        }

        test("OslcMixin should not complain if green") {
            shouldNotThrowAny {
                checkMixin.checkOslcCompliance(report.name, listOf(report), false)
            }
        }

        test("OslcMixin should complain about distribution") {
            val modifiedReport = report.copy(
                test = (report.test as OslcTestResult).copy(
                    policyProfile = "Distribution"
                )
            )
            shouldThrow<IllegalStateException> {
                checkMixin.checkOslcCompliance(modifiedReport.name, listOf(modifiedReport), false)
            }
        }
    }
}
