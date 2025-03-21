package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockkObject

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class NameProviderJenkinsTest(platformProvider: DefaultNameProviderFactory) : AnnotationSpec() {
    private val provider = platformProvider.getProvider(PlatformType.JENKINS)
    private val resolver = provider.resolver
    private val result: String by lazy { provider.provideNames() }

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "JOB_NAME" to "SDM/cli/i593-cli-createnames",
                "BRANCH_NAME" to "i593-cli-createnames",
                "GIT_BRANCH" to "i593-cli-createnames",
                "BUILD_NUMBER" to "25",
                "GIT_COMMIT" to "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
                "JOB_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval/",
                "JOB_DISPLAY_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval",
                "RUN_DISPLAY_URL" to "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval/26/display/redirect",
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
        result shouldContainIgnoringCase "${Names.CDLIB_APP_NAME}=$value"
    }

    @Test
    fun testProvide_APP_VERSION() {
        val value = resolver[Names.CDLIB_APP_VERSION]
        result shouldContainIgnoringCase "${Names.CDLIB_APP_VERSION}=$value"
    }

    @Test
    fun testProvide_CHART_VERSION() {
        val value = resolver[Names.CDLIB_CHART_VERSION]
        result shouldContainIgnoringCase "${Names.CDLIB_CHART_VERSION}=$value"
    }

    @Test
    fun testProvide_CHART_VERSION_OCI() {
        val value = resolver[Names.CDLIB_CHART_VERSION_OCI]
        result shouldContainIgnoringCase "${Names.CDLIB_CHART_VERSION_OCI}=$value"
    }

    @Test
    fun testProvide_CONTAINER_TAG() {
        val value = resolver[Names.CDLIB_CONTAINER_TAG]
        result shouldContainIgnoringCase "${Names.CDLIB_CONTAINER_TAG}=$value"
    }

    @Test
    fun testProvide_EFFECTIVE_BRANCH_NAME() {
        val value = resolver[Names.CDLIB_EFFECTIVE_BRANCH_NAME]
        result shouldContainIgnoringCase "${Names.CDLIB_EFFECTIVE_BRANCH_NAME}=$value"
    }

    @Test
    fun testProvide_JOB_URL() {
        val value = resolver[Names.CDLIB_JOB_URL]
        result shouldContainIgnoringCase "${Names.CDLIB_JOB_URL}=$value"
    }

    @Test
    fun testProvide_PIPELINE_URL() {
        val value = resolver[Names.CDLIB_PIPELINE_URL]
        result shouldContainIgnoringCase "${Names.CDLIB_PIPELINE_URL}=$value"
    }

    @Test
    fun testProvide_PM_GIT_ID() {
        val value = resolver[Names.CDLIB_PM_GIT_ID]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_ID}=$value"
    }

    @Test
    fun testProvide_PM_GIT_LINK() {
        val value = resolver[Names.CDLIB_PM_GIT_LINK]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_LINK}=$value"
    }

    @Test
    fun testProvide_PM_GIT_MAIL() {
        val value = resolver[Names.CDLIB_PM_GIT_MAIL]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_MAIL}=$value"
    }

    @Test
    fun testProvide_PM_GIT_MESSAGE() {
        val value = resolver[Names.CDLIB_PM_GIT_MESSAGE]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_MESSAGE}=$value"
    }

    @Test
    fun testProvide_PM_GIT_NAME() {
        val value = resolver[Names.CDLIB_PM_GIT_NAME]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_NAME}=$value"
    }

    @Test
    fun testProvide_PM_GIT_ORIGIN() {
        val value = resolver[Names.CDLIB_PM_GIT_ORIGIN]
        result shouldContainIgnoringCase "${Names.CDLIB_PM_GIT_ORIGIN}=$value"
    }

    @Test
    fun testProvide_RELEASE_NAME() {
        val value = resolver[Names.CDLIB_RELEASE_NAME]
        result shouldContainIgnoringCase "${Names.CDLIB_RELEASE_NAME}=$value"
    }

    @Test
    fun testProvide_RELEASE_NAME_FORTIFY() {
        val value = resolver[Names.CDLIB_RELEASE_NAME_FORTIFY]
        result shouldContainIgnoringCase "${Names.CDLIB_RELEASE_NAME_FORTIFY}=$value"
    }

    @Test
    fun testProvide_RELEASE_NAME_HELM() {
        val value = resolver[Names.CDLIB_RELEASE_NAME_HELM]
        result shouldContainIgnoringCase "${Names.CDLIB_RELEASE_NAME_HELM}=$value"
    }

    @Test
    fun testProvide_RELEASE_VERSION() {
        val value = resolver[Names.CDLIB_RELEASE_VERSION]
        result shouldContainIgnoringCase "${Names.CDLIB_RELEASE_VERSION}=$value"
    }

    @Test
    fun testProvide_REVISION() {
        val value = resolver[Names.CDLIB_REVISION]
        result shouldContainIgnoringCase "${Names.CDLIB_REVISION}=$value"
    }

    @Test
    fun testProvide_SANITIZED_BRANCH_NAME() {
        val value = resolver[Names.CDLIB_SANITIZED_BRANCH_NAME]
        result shouldContainIgnoringCase "${Names.CDLIB_SANITIZED_BRANCH_NAME}=$value"
    }

    @Test
    fun testProvide_SEMANTIC_VERSION() {
        val value = resolver[Names.CDLIB_SEMANTIC_VERSION]
        result shouldContainIgnoringCase "${Names.CDLIB_SEMANTIC_VERSION}=$value"
    }

    @Test
    fun testProvide_TERRAFORM_PREFIX() {
        val value = resolver[Names.CDLIB_TERRAFORM_PREFIX]
        result shouldContainIgnoringCase "${Names.CDLIB_TERRAFORM_PREFIX}=$value"
    }
}
