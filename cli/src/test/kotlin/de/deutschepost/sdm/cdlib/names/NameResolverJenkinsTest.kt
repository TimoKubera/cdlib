package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.extensions.time.withConstantNow
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.*
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockkObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class NameResolverJenkinsTest(private val resolver: NameResolverJenkins) : AnnotationSpec() {
    private val before = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant()

    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "JOB_NAME" to "SDM/cli/i593-cli-CreateNames",
                "BRANCH_NAME" to "i593-cli-CreateNames",
                "GIT_BRANCH" to "i593-cli-CreateNames",
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
        every { GitRepository.getRemoteUrl(any()) } returns "https://git.dhl.com/CDLib/CDLib.git"
        every { GitRepository.lastBranchCommit(any()) } returns GitRevision(
            id = "a5c5bc3ce1907e844490697b9aa22c4196c5d781",
            longMessage = "Dummy \"Commit\"",
            shortMessage = "Dummy \$Commit",
            authorName = "Firstname Author",
            authorEmail = "f.a@dhl.com",
            committerName = "Firstname Committer",
            committerEmail = "f.c@dhl.com"
        )
    }

    @Test
    fun testCreate_APP_NAME() {
        // Since "BRANCH_NAME" is defined expect "cli" from  "SDM/cli/i593-cli-createnames"
        resolver[Names.CDLIB_APP_NAME] shouldBeEqualIgnoringCase "cli"
    }

    @Test
    fun testCreate_APP_VERSION() {
        withConstantNow(ZonedDateTime.of(2022, 1, 1, 0, 2, 4, 4, ZoneId.systemDefault())) {
            resolver[Names.CDLIB_APP_VERSION] shouldBe "20220101.2.4"
        }
    }

    @Test
    fun testCreate_CHART_VERSION() {
        resolver[Names.CDLIB_CHART_VERSION] shouldHaveMaxLength 35
        resolver[Names.CDLIB_CHART_VERSION] shouldContainIgnoringCase "-i593"
    }

    @Test
    fun testCreate_CHART_VERSION_OCI() {
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldHaveMaxLength 35
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldContainIgnoringCase "-i593"
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldEndWith "-helm"
    }

    @Test
    fun testCreate_CONTAINER_TAG() {
        resolver[Names.CDLIB_CONTAINER_TAG] shouldHaveMaxLength 35
        resolver[Names.CDLIB_CONTAINER_TAG] shouldContainIgnoringCase "-i593"
    }

    @Test
    fun testCreate_EFFECTIVE_BRANCH_NAME() {
        resolver[Names.CDLIB_EFFECTIVE_BRANCH_NAME] shouldBeEqualIgnoringCase "i593-cli-createnames"
    }

    @Test
    fun testCreate_JOB_URL() {
        resolver[Names.CDLIB_JOB_URL] shouldBeEqualIgnoringCase "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval/26/display/redirect"
    }

    @Test
    fun testCreate_PIPELINE_URL() {
        resolver[Names.CDLIB_PIPELINE_URL] shouldBeEqualIgnoringCase "https://jenkins.dhl.com/job/SDM/job/cli/job/i613-record-webapproval"
    }

    @Test
    fun testCreate_PM_VALUES() {
        resolver[Names.CDLIB_PM_GIT_ID] shouldBeEqualIgnoringCase "a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        resolver[Names.CDLIB_PM_GIT_MAIL] shouldBeEqualIgnoringCase "f.c@dhl.com"
        resolver[Names.CDLIB_PM_GIT_MESSAGE] shouldBeEqualIgnoringCase "Dummy Commit"
        resolver[Names.CDLIB_PM_GIT_NAME] shouldBeEqualIgnoringCase "Firstname Committer"
        resolver[Names.CDLIB_PM_GIT_ORIGIN] shouldBeEqualIgnoringCase "https://git.dhl.com/CDLib/CDLib.git"
        resolver[Names.CDLIB_PM_GIT_LINK] shouldBeEqualIgnoringCase "https://git.dhl.com/CDLib/CDLib/commit/a5c5bc3ce1907e844490697b9aa22c4196c5d781"
    }

    @Test
    fun testCreate_RELEASE_VERSION() {
        resolver[Names.CDLIB_RELEASE_VERSION] shouldContainIgnoringCase "25_a5c5bc3"
    }

    @Test
    fun testCreate_RELEASE_NAME() {
        resolver[Names.CDLIB_RELEASE_NAME] shouldContainIgnoringCase "25_a5c5bc3"
        resolver[Names.CDLIB_RELEASE_NAME] shouldContainIgnoringCase "cli"
    }

    @Test
    fun testCreate_RELEASE_NAME_FORTIFY() {
        resolver[Names.CDLIB_RELEASE_NAME_FORTIFY] shouldContainIgnoringCase "25_a5c5bc3"
        resolver[Names.CDLIB_RELEASE_NAME_FORTIFY] shouldContainIgnoringCase "cli"
    }

    @Test
    fun testCreate_RELEASE_NAME_HELM() {
        resolver[Names.CDLIB_RELEASE_NAME_HELM] shouldHaveMaxLength 53
        resolver[Names.CDLIB_RELEASE_NAME_HELM] shouldContain "cli-i593"
        resolver[Names.CDLIB_RELEASE_NAME_HELM] shouldContain "createnames"
    }

    @Test
    fun testCreate_REVISION() {
        resolver[Names.CDLIB_REVISION] shouldBeEqualIgnoringCase "a5c5bc3"
    }

    @Test
    fun testCreate_SANITIZED_BRANCH_NAME() {
        resolver[Names.CDLIB_SANITIZED_BRANCH_NAME] shouldBe "i593-cli-createnames"
    }

    @Test
    fun testCreate_SEMANTIC_VERSION() {
        resolver[Names.CDLIB_SEMANTIC_VERSION] shouldContainIgnoringCase "i593-cli-createnames.25.a5c5bc3"
    }

    @Test
    fun testCreate_TERRAFORM_PREFIX() {
        resolver[Names.CDLIB_TERRAFORM_PREFIX] shouldBe "CDLib-i593-cli-CreateNames"
    }

    @Test
    fun testCreate_RELEASE_NAME_UNIQUE() {
        val releaseNameUnique = resolver[Names.CDLIB_RELEASE_NAME_UNIQUE]
        releaseNameUnique shouldContain "25_a5c5bc3"
        releaseNameUnique shouldContain "cli"
        val date =
            ZonedDateTime.parse(releaseNameUnique.split("_").last(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        val after = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant()
        date shouldBeGreaterThanOrEqualTo before
        date shouldBeLessThanOrEqualTo after
    }
}
