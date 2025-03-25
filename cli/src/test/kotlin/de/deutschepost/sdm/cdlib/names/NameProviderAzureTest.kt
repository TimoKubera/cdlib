package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContain
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockkObject

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class NameProviderAzureTest(platformProvider: DefaultNameProviderFactory) : AnnotationSpec() {
    private val provider = platformProvider.getProvider(PlatformType.AZURE_DEVOPS)
    private val resolver = provider.resolver
    private val result: String by lazy { provider.provideNames() }

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "BUILD_BUILDID" to "12657",
                "BUILD_DEFINITIONNAME" to "ICTO-3339_SDM-phippyandfriends",
                "BUILD_SOURCEBRANCHNAME" to "i593_test",
                "SYSTEM_COLLECTIONURI" to "https://dev.azure.com/sw-zustellung-31b3183/",
                "SYSTEM_TEAMPROJECT" to "ICTO-3339_SDM",
                "SYSTEM_DEFINITIONID" to "912345",
                "SYSTEM_PULLREQUEST_SOURCEBRANCH" to "branchName"
            ), OverrideMode.SetOrOverride
        )
    )

    @BeforeAll
    fun initMocks() {
        mockkObject(GitRepository)
        every { GitRepository.getRemoteUrl(any()) } returns "https://git.dhl.com/CDLib/CDlib.git"
        every { GitRepository.lastBranchCommit(any()) } returns GitRevision(
            id = "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
            longMessage = "Dummy Commit",
            shortMessage = "Dummy Commit",
            authorName = "Firstname Lastname",
            authorEmail = "f.l@dhl.com",
            committerName = "Firstname Lastname",
            committerEmail = "f.l@dhl.com"
        )
    }

    @Test
    fun testProvide_Size() {
        val entries = result.split("\n").filter { it.isNotBlank() }
        entries.size shouldBeExactly Names.values().size
    }

    @Test
    fun testProvide_APP_NAME() {
        val value = resolver[Names.CDLIB_APP_NAME]
        result shouldContain "##vso[task.setvariable variable=CDLIB_APP_NAME;isOutput=true]$value"

    }

    @Test
    fun testProvide_APP_VERSION() {
        val value = resolver[Names.CDLIB_APP_VERSION]
        result shouldContain "##vso[task.setvariable variable=CDLIB_APP_VERSION;isOutput=true]$value"
    }

    @Test
    fun testProvide_CHART_VERSION() {
        val value = resolver[Names.CDLIB_CHART_VERSION]
        result shouldContain "##vso[task.setvariable variable=CDLIB_CHART_VERSION;isOutput=true]$value"
    }

    @Test
    fun testProvide_CHART_VERSION_OCI() {
        val value = resolver[Names.CDLIB_CHART_VERSION_OCI]
        result shouldContain "##vso[task.setvariable variable=CDLIB_CHART_VERSION_OCI;isOutput=true]$value"
    }

    @Test
    fun testProvide_CONTAINER_TAG() {
        val value = resolver[Names.CDLIB_CONTAINER_TAG]
        result shouldContain "##vso[task.setvariable variable=CDLIB_CONTAINER_TAG;isOutput=true]$value"
    }

    @Test
    fun testProvide_EFFECTIVE_BRANCH_NAME() {
        val value = resolver[Names.CDLIB_EFFECTIVE_BRANCH_NAME]
        result shouldContain "##vso[task.setvariable variable=CDLIB_EFFECTIVE_BRANCH_NAME;isOutput=true]$value"
    }

    @Test
    fun testProvide_JOB_URL() {
        val value = resolver[Names.CDLIB_JOB_URL]
        result shouldContain "##vso[task.setvariable variable=CDLIB_JOB_URL;isOutput=true]$value"
    }

    @Test
    fun testProvide_PIPELINE_URL() {
        val value = resolver[Names.CDLIB_PIPELINE_URL]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PIPELINE_URL;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_ID() {
        val value = resolver[Names.CDLIB_PM_GIT_ID]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_ID;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_LINK() {
        val value = resolver[Names.CDLIB_PM_GIT_LINK]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_LINK;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_MAIL() {
        val value = resolver[Names.CDLIB_PM_GIT_MAIL]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_MAIL;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_MESSAGE() {
        val value = resolver[Names.CDLIB_PM_GIT_MESSAGE]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_MESSAGE;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_NAME() {
        val value = resolver[Names.CDLIB_PM_GIT_NAME]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_NAME;isOutput=true]$value"
    }

    @Test
    fun testProvide_PM_GIT_ORIGIN() {
        val value = resolver[Names.CDLIB_PM_GIT_ORIGIN]
        result shouldContain "##vso[task.setvariable variable=CDLIB_PM_GIT_ORIGIN;isOutput=true]$value"
    }

    @Test
    fun testProvide_RELEASE_NAME() {
        val value = resolver[Names.CDLIB_RELEASE_NAME]
        result shouldContain "##vso[task.setvariable variable=CDLIB_RELEASE_NAME;isOutput=true]$value"
    }

    @Test
    fun testProvide_RELEASE_NAME_FORTIFY() {
        val value = resolver[Names.CDLIB_RELEASE_NAME_FORTIFY]
        result shouldContain "##vso[task.setvariable variable=CDLIB_RELEASE_NAME_FORTIFY;isOutput=true]$value"
    }

    @Test
    fun testProvide_RELEASE_NAME_HELM() {
        val value = resolver[Names.CDLIB_RELEASE_NAME_HELM]
        result shouldContain "##vso[task.setvariable variable=CDLIB_RELEASE_NAME_HELM;isOutput=true]$value"
    }

    @Test
    fun testProvide_RELEASE_VERSION() {
        val value = resolver[Names.CDLIB_RELEASE_VERSION]
        result shouldContain "##vso[task.setvariable variable=CDLIB_RELEASE_VERSION;isOutput=true]$value"
    }

    @Test
    fun testProvide_REVISION() {
        val value = resolver[Names.CDLIB_REVISION]
        result shouldContain "##vso[task.setvariable variable=CDLIB_REVISION;isOutput=true]$value"
    }

    @Test
    fun testProvide_SANITIZED_BRANCH_NAME() {
        val value = resolver[Names.CDLIB_SANITIZED_BRANCH_NAME]
        result shouldContain "##vso[task.setvariable variable=CDLIB_SANITIZED_BRANCH_NAME;isOutput=true]$value"
    }

    @Test
    fun testProvide_SEMANTIC_VERSION() {
        val value = resolver[Names.CDLIB_SEMANTIC_VERSION]
        result shouldContain "##vso[task.setvariable variable=CDLIB_SEMANTIC_VERSION;isOutput=true]$value"
    }

    @Test
    fun testProvide_TERRAFORM_PREFIX() {
        val value = resolver[Names.CDLIB_TERRAFORM_PREFIX]
        result shouldContain "##vso[task.setvariable variable=CDLIB_TERRAFORM_PREFIX;isOutput=true]$value"
    }
}
