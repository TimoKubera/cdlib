package de.deutschepost.sdm.cdlib.release

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ITS_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.release.report.TestResultParser
import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.from
import de.deutschepost.sdm.cdlib.utils.sha256sum
import exists
import io.kotest.assertions.withClue
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.extensions.time.withConstantNow
import io.kotest.matchers.sequences.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.jfrog.artifactory.client.Artifactory
import toArgsArray
import withErrorOutput
import withStandardOutput
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ReportUploadCommandIntegrationTest(@Value("\${artifactory-its-identity-token}") val artifactoryIdentityToken: String) :
    FunSpec() {
    private val repoName = "sdm-proj-prg-cdlib-cli-appimage"
    private val releaseName = "cdlib-cli-integration_42.1337.42"
    private val releaseName_build = releaseName + "_build"

    private val artifactoryClient = ArtifactoryClient(artifactoryIdentityToken, ITS_ARTIFACTORY_URL)
    private val artifactory by lazy {
        val declaredField = artifactoryClient.javaClass.getDeclaredField("artifactory")
        declaredField.isAccessible = true
        declaredField.get(artifactoryClient) as Artifactory
    }

    init {
        val repositoryHandle = artifactory.repository(repoName)

        val uploadArgs =
            "--repo-name $repoName --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --type build --folder-name $releaseName --debug "
        test("Don't fail up-to-date reports and upload both.") {
            withConstantNow(LocalDate.of(1900, 1, 1).atStartOfDay(ZoneId.systemDefault())) {
                val testFile = "src/test/resources/zap/_zap-report_with_open_outdated.json"
                withClue("Folder $releaseName_build should not exist before test") {
                    repositoryHandle.folder(releaseName_build).exists() shouldBe false
                }
                val args =
                    (uploadArgs +
                        "--report-prefix-zap _zap --files $testFile").toArgsArray()
                val (ret, output) = withStandardOutput {
                    PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                }
                ret shouldBe 0

                val testResult = TestResultParser.parse(File(testFile), TestResultPrefixes(zap = "_zap"))
                requireNotNull(testResult)
                val report = Report(test = testResult, testHash = testFile.sha256sum())
                repositoryHandle.file("$releaseName_build/${report.canonicalFilename}").exists() shouldBe true
            }
        }

        test("Don't upload if files are outdated.") {
            val testFile = "src/test/resources/zap/zap-report_all_suppressed.json"
            withClue("Folder $releaseName_build should not exist before test") {
                repositoryHandle.folder(releaseName_build).exists() shouldBe false
            }
            val args = (uploadArgs +
                "--files src/test/resources/zap/zap-report_with_open.json --files $testFile").toArgsArray()
            val ret = PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
            ret shouldBe -1

            val testResult = TestResultParser.parse(File(testFile))
            requireNotNull(testResult)
            val report = Report(test = testResult, testHash = testFile.sha256sum())
            repositoryHandle.file("$releaseName_build/${report.canonicalFilename}").exists() shouldBe false

        }

        test("Don't upload if params are missing.") {
            val testFile = "src/test/resources/zap/zap-report_all_suppressed.json"
            withClue("Folder $releaseName_build should not exist before test") {
                repositoryHandle.folder(releaseName_build).exists() shouldBe false
            }
            val args =
                ("--artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --folder-name $releaseName " +
                    "--files src/test/resources/zap/zap-report_with_open.json --files $testFile").toArgsArray()
            val (ret, output) = withErrorOutput {
                PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
            }
            ret shouldBe null
            output shouldContain "Missing required option"

        }

        test("Upload multiple files") {
            withConstantNow(LocalDate.of(2021, 10, 31).atStartOfDay(ZoneId.systemDefault())) {
                val parent =
                    "src${File.separatorChar}test${File.separatorChar}resources${File.separatorChar}passing"
                withClue("Folder $releaseName_build should not exist before test") {
                    repositoryHandle.folder(releaseName_build).exists() shouldBe false
                }
                val args = (uploadArgs +
                    "--files $parent${File.separatorChar}*").toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                ret shouldBe 0

                val reports = File(parent).walk().mapNotNull {
                    TestResultParser.parse(it)?.let { parsed -> Report(test = parsed, testHash = it.sha256sum()) }
                }.map { report ->
                    if (report.test.reportType == ReportType.OSLC_PRE) {
                        report.copy(test = OslcTestResult.from(report.test as OslcTestPreResult, false))
                    } else {
                        report
                    }
                }
                withClue(reports) { reports.shouldHaveSize(7) }


                reports.forEach { report ->
                    withClue("$releaseName_build/${report.canonicalFilename} should exist") {
                        repositoryHandle.file("$releaseName_build/${report.canonicalFilename}")
                            .exists() shouldBe true
                    }
                }
            }
        }

        test("Alternate names") {
            val alternateUploadArgs =
                "--repo-name $repoName --artifactory-its-instance --artifactory-identity-token $artifactoryIdentityToken --type build --folder-name $releaseName --debug "
            withConstantNow(LocalDate.of(1900, 1, 1).atStartOfDay(ZoneId.systemDefault())) {
                val testFile = "src/test/resources/zap/_zap-report_with_open_outdated.json"
                withClue("Folder $releaseName_build should not exist before test") {
                    repositoryHandle.folder(releaseName_build).exists() shouldBe false
                }
                val args =
                    (alternateUploadArgs +
                        "--report-prefix-zap _zap --files $testFile").toArgsArray()
                val ret = PicocliRunner.call(ReportCommand.UploadCommand::class.java, *args)
                ret shouldBe 0

                val testResult = TestResultParser.parse(File(testFile), TestResultPrefixes(zap = "_zap"))
                requireNotNull(testResult)
                val report = Report(test = testResult, testHash = testFile.sha256sum())
                repositoryHandle.file("$releaseName_build/${report.canonicalFilename}").exists() shouldBe true
            }
        }
    }


    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "CDLIB_RELEASE_NAME_UNIQUE" to "cli-integration-test-${System.currentTimeMillis()}",
                "CDLIB_RELEASE_NAME" to "cli-integration-test",
                "CDLIB_APP_NAME" to "cli",
                "CDLIB_EFFECTIVE_BRANCH_NAME" to "integration-test-branch",
                "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
                "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
                "CDLIB_PM_GIT_ID" to "integration-test-git-id",
                "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
                "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
                "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
                "CDLIB_CICD_PLATFORM" to "integration-test-platform",
                "CDLIB_JOB_URL" to "integration-test-platform.com/integration-test"
            ), OverrideMode.SetOrOverride
        )
    )

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        val repository = artifactory.repository(repoName)
        try {
            repository.delete(releaseName_build)
        } catch (e: Exception) {
            println(e)
        }

    }
}
