package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.micronaut.configuration.picocli.PicocliRunner
import io.mockk.every
import io.mockk.mockkObject
import toArgsArray
import withStandardOutput

@RequiresTag("UnitTest")
@Tags("UnitTest")
class NamesCommandAzureTest : AnnotationSpec() {

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "BUILD_BUILDID" to "12657",
                "BUILD_DEFINITIONNAME" to "ICTO-3339_SDM-phippyandfriends",
                "BUILD_SOURCEBRANCHNAME" to "i593_test",
                "SYSTEM_COLLECTIONURI" to "https://dev.azure.com/sw-zustellung-31b3183/",
                "SYSTEM_TEAMPROJECT" to "ICTO-3339_SDM",
                "SYSTEM_PULLREQUEST_SOURCEBRANCH" to "",
                "BUILD_SOURCEBRANCH" to "refs/heads/i593_test",
                "SYSTEM_DEFINITIONID" to "101010",
                // Explicit set / reset var. relevant for platform detection
                "TF_BUILD" to "True",
                "JENKINS_HOME" to "",
            ), OverrideMode.SetOrOverride
        )
    )

    @BeforeAll
    fun initMocks() {
        mockkObject(GitRepository)
        every { GitRepository.getRemoteUrl(any()) } returns "https://git.dhl.com/CDLib/CDLib.git"
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
    
        output shouldContainIgnoringCase CDLIB_APP_NAME_VARIABLE
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_EFFECTIVE_BRANCH_NAME;isOutput=true]i593_test"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_PM_GIT_ID;isOutput=true]a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_RELEASE_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends_"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_REVISION;isOutput=true]a5c5bc3"
    }
    
    companion object {
        const val CDLIB_APP_NAME_VARIABLE = "##vso[task.setvariable variable=CDLIB_APP_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends"
    }

    @Test
    fun testNamesCreateOverrideOrigin() {
        val origin = "https://git.dhl.com/Overriden/Origin.git"
        val (_, output) = withStandardOutput {
            val args = "names create --override-origin $origin".toArgsArray()
            PicocliRunner.run(CdlibCommand::class.java, *args)
        }
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_PM_GIT_ORIGIN;isOutput=true]$origin"
    }

    @Test
    fun testNamesCreateWithValidReleaseName() {
        val (_, output) = withStandardOutput {
            val releaseName = "ICTO-3339_SDM-phippyandfriends_20211203.1809.18_20210908.16_a5c5bc3"
            val args = "names create --from-release-name $releaseName".toArgsArray()
            PicocliRunner.run(CdlibCommand::class.java, *args)
        }
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_APP_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_APP_VERSION;isOutput=true]20211203.1809.18"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_BUILD_NUMBER;isOutput=true]20210908.16"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_EFFECTIVE_BRANCH_NAME;isOutput=true]i593_test"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_PM_GIT_ID;isOutput=true]a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_RELEASE_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends_"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_RELEASE_VERSION;isOutput=true]20211203.1809.18_20210908.16_a5c5bc3"
        output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_REVISION;isOutput=true]a5c5bc3"
    }

    @Test
    fun testNamesCreateWithInvalidReleaseName() {
        val releaseName = "ICTO-20211203.1809.18_12657_a5c5bc3"
        val args = "--from-release-name $releaseName".toArgsArray()

        val returnCode = PicocliRunner.call(NamesCommand.CreateCommand::class.java, *args)
        returnCode shouldBeEqualComparingTo -1
    }

    @Test
    fun testPREffectiveBranchName() {
        withEnvironment(
            mapOf(
                "SYSTEM_PULLREQUEST_SOURCEBRANCH" to "refs/heads/i593_test",
                "BUILD_SOURCEBRANCHNAME" to "merge",
            ),
            OverrideMode.SetOrOverride
        ) {
            val (_, output) = withStandardOutput {
                val args = "names create".toArgsArray()
                PicocliRunner.run(CdlibCommand::class.java, *args)
            }

            output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_APP_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends"
            output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_EFFECTIVE_BRANCH_NAME;isOutput=true]i593_test"
            output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_PM_GIT_ID;isOutput=true]a5c5bc3ce1907e844490697b9aa22c4196c5d781"
            output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_RELEASE_NAME;isOutput=true]ICTO-3339_SDM-phippyandfriends_"
            output shouldContainIgnoringCase "##vso[task.setvariable variable=CDLIB_REVISION;isOutput=true]a5c5bc3"
        }

    }
}
