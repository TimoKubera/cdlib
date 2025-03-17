package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.eclipse.jgit.lib.Repository
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class NamesCommandJenkinsTest : AnnotationSpec() {

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "JOB_NAME" to "SDM/cli/i593-cli-createnames",
                "BRANCH_NAME" to "i593-cli-createnames",
                "GIT_BRANCH" to "i593-cli-createnames",
                "BUILD_NUMBER" to "25",
                "GIT_COMMIT" to "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
                "JOB_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval/",
                "RUN_DISPLAY_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval/26/display/redirect",
                "JOB_DISPLAY_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval",
                // Explicit set / reset var. relevant for platform detection
                "JENKINS_HOME" to "/var/lib/jenkins",
                "TF_BUILD" to "",
            ), OverrideMode.SetOrOverride
        )
    )

    @BeforeAll
    fun initMocks() {
        mockkObject(GitRepository, recordPrivateCalls = true)
        val repo = mockk<Repository>()
        every { GitRepository invoke "getRepository" withArguments listOf(any<String>()) } returns repo
        every { repo.config.getString(any(), any(), any()) } returns "https://foo:bar@git.dhl.com/CDLib/CDLib.git"
        every { GitRepository.lastBranchCommit(any()) } returns GitRevision(
            id = "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
            longMessage = "Dummy Commit",
            shortMessage = "Dummy Commit",
            authorName = "Firstname Author",
            authorEmail = "f.a@dhl.com",
            committerName = "Firstname Committer",
            committerEmail = "f.c@dhl.com"
        )
    }

    @Test
    fun testNamesCreate() {
        val (_, output) = withStandardOutput {
            val args = "names create".toArgsArray()
            PicocliRunner.run(CdlibCommand::class.java, *args)
        }
        output shouldContainIgnoringCase "CDLIB_APP_NAME=cli"
        output shouldContainIgnoringCase "CDLIB_BUILD_NUMBER=25"
        output shouldContainIgnoringCase "CDLIB_EFFECTIVE_BRANCH_NAME=i593-cli-createnames"
        output shouldContainIgnoringCase "CDLIB_PM_GIT_ID=a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        output shouldContainIgnoringCase "CDLIB_REVISION=a5c5bc3"
        output shouldContainIgnoringCase "CDLIB_PM_GIT_LINK=https://git.dhl.com/CDLib/CDLib/commit/a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        output shouldContainIgnoringCase "CDLIB_PM_GIT_ORIGIN=https://git.dhl.com/CDLib/CDLib.git"
        output shouldContainIgnoringCase "CDLIB_NAMES_CREATE_SUCCESS=1"

    }

    @Test
    fun testNamesCreateOverrideOrigin() {
        val origin = "https://git.dhl.com/Overriden/Origin.git"
        val (_, output) = withStandardOutput {
            val args = "names create --override-origin $origin".toArgsArray()
            PicocliRunner.run(CdlibCommand::class.java, *args)
        }
        output shouldContainIgnoringCase "CDLIB_PM_GIT_ORIGIN=$origin"
    }

    @Test
    fun testNamesCreateWithValidReleaseName() {
        val (_, output) = withStandardOutput {
            val releaseName = "cli_20211203.1827.54_26_a5c5bc3"
            val args = "names create --from-release-name $releaseName".toArgsArray()
            PicocliRunner.run(CdlibCommand::class.java, *args)
        }
        output shouldContainIgnoringCase "CDLIB_APP_NAME=cli"
        output shouldContainIgnoringCase "CDLIB_APP_VERSION=20211203.1827.54"
        output shouldContainIgnoringCase "CDLIB_BUILD_NUMBER=26"
        output shouldContainIgnoringCase "CDLIB_EFFECTIVE_BRANCH_NAME=i593-cli-createnames"
        output shouldContainIgnoringCase "CDLIB_PM_GIT_ID=a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        output shouldContainIgnoringCase "CDLIB_RELEASE_NAME=cli_20211203.1827.54_26_a5c5bc3"
        output shouldContainIgnoringCase "CDLIB_RELEASE_VERSION=20211203.1827.54_26_a5c5bc3"
        output shouldContainIgnoringCase "CDLIB_REVISION=a5c5bc3"
        output shouldContainIgnoringCase "CDLIB_NAMES_CREATE_SUCCESS=1"
    }

    @Test
    fun testNamesCreateWithInvalidReleaseName() {
        val releaseName = "cli-20211203.1827.54_25_a5c5bc3"
        val args = "--from-release-name $releaseName".toArgsArray()

        val returnCode = PicocliRunner.call(NamesCommand.CreateCommand::class.java, *args)
        returnCode shouldBeEqualComparingTo -1
    }
}
