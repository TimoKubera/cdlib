package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.names.git.GitRepository
import de.deutschepost.sdm.cdlib.names.git.GitRevision
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.kotest.extensions.system.withEnvironment
import io.kotest.extensions.time.withConstantNow
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
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
class NameResolverAzureTest(
    private val resolver: NameResolverAzure,
    private val namesConfigWithDefault: NamesConfigWithDefault
) : AnnotationSpec() {
    companion object {
        private const val BUILD_DEFINITION_NAME = "ICTO-3339_SDM-phippyandfriends"
    }
) : AnnotationSpec() {
    private val before = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant()


    override fun listeners() = listOf(
        SystemEnvironmentTestListener(
            mapOf(
                "BUILD_BUILDID" to "12657",
                "BUILD_DEFINITIONNAME" to "ICTO-3339_SDM-phippyandfriends",
                "BUILD_SOURCEBRANCHNAME" to "i593_test",
                "SYSTEM_COLLECTIONURI" to "https://dev.azure.com/sw-zustellung-31b3183/",
                "SYSTEM_TEAMPROJECT" to "ICTO-3339_SDM",
                "TF_BUILD" to "True",
                "SYSTEM_DEFINITIONID" to "912345",
                "SYSTEM_PULLREQUEST_SOURCEBRANCH" to "i593_test"
            ),
            OverrideMode.SetOrOverride
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
    fun testCreate_sanitizedTruncated() {
        withEnvironment(
            mapOf(
                "BUILD_SOURCEBRANCHNAME" to "feature/THIS-is-a-very-long-branch-name-that-needs-to-be-truncated",
                "CDLIB_EFFECTIVE_BRANCH_NAME" to "feature/THIS-is-a-very-long-branch-name-that-needs-to-be-truncated"
            ), OverrideMode.SetOrOverride
        ) {
            val newResolver = NameResolverAzure(namesConfigWithDefault)
            newResolver[Names.CDLIB_RELEASE_NAME_HELM] shouldBe "icto-3339_sdm-phippyandfriends-feature-this-is-a-very"
            newResolver[Names.CDLIB_RELEASE_NAME_HELM].length shouldBe 53
        }
        withEnvironment(
            mapOf(
                "BUILD_SOURCEBRANCHNAME" to "renovate/THIS-is-a-very-long-branch-name-that-needs-to-be-truncated",
                "CDLIB_EFFECTIVE_BRANCH_NAME" to "renovate/THIS-is-a-very-long-branch-name-that-needs-to-be-truncated"
            ), OverrideMode.SetOrOverride
        ) {
            val newResolver = NameResolverAzure(namesConfigWithDefault)
            newResolver[Names.CDLIB_RELEASE_NAME_HELM] shouldBe "icto-3339_sdm-phippyandfriends--needs-to-be-truncated"
            newResolver[Names.CDLIB_RELEASE_NAME_HELM].length shouldBe 53
        }
        withEnvironment(
            mapOf(
                "BUILD_DEFINITIONNAME" to "ICTO-3339_SDM-phippyandfriends-very-long-app-name-that-needs-to-be-truncated",
                "BUILD_SOURCEBRANCHNAME" to "renovate/this-is-a-very-long-branch-name-that-needs-to-be-truncated",
                "CDLIB_EFFECTIVE_BRANCH_NAME" to "renovate/this-is-a-very-long-branch-name-that-needs-to-be-truncated"
            ), OverrideMode.SetOrOverride
        ) {
            val newResolver = NameResolverAzure(namesConfigWithDefault)
            newResolver[Names.CDLIB_RELEASE_NAME_HELM] shouldBe "icto-3339_sdm-phippyandfriends-very-long-app-name-tha"
            newResolver[Names.CDLIB_RELEASE_NAME_HELM].length shouldBe 53
        }
    }

    @Test
    fun testCreate_APP_NAME() {
        resolver[Names.CDLIB_APP_NAME] shouldBeEqualComparingTo "ICTO-3339_SDM-phippyandfriends"
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
        resolver[Names.CDLIB_CHART_VERSION] shouldContain "-i593"
    }

    @Test
    fun testCreate_CHART_VERSION_OCI() {
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldHaveMaxLength 35
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldContain "-i593"
        resolver[Names.CDLIB_CHART_VERSION_OCI] shouldEndWith "-helm"
    }

    @Test
    fun testCreate_CONTAINER_TAG() {
        resolver[Names.CDLIB_CONTAINER_TAG] shouldHaveMaxLength 35
        resolver[Names.CDLIB_CONTAINER_TAG] shouldContain "-i593"
    }

    @Test
    fun testCreate_EFFECTIVE_BRANCH_NAME() {
        resolver[Names.CDLIB_EFFECTIVE_BRANCH_NAME] shouldBeEqualIgnoringCase "i593_test"
    }

    @Test
    fun testCreate_JOB_URL() {
        resolver[Names.CDLIB_JOB_URL] shouldContainIgnoringCase "https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_build/results?buildId=12657"
    }

    @Test
    fun testCreate_PIPELINE_URL_pullRequestSourceBranchName() {
        withEnvironment(
            mapOf(
                "BUILD_SOURCEBRANCHNAME" to "merge",
                "SYSTEM_PULLREQUEST_SOURCEBRANCH" to "i593_test"
            ),
            OverrideMode.SetOrOverride
        ) {
            resolver[Names.CDLIB_PIPELINE_URL] shouldContainIgnoringCase "912345"
            resolver[Names.CDLIB_PIPELINE_URL] shouldContainIgnoringCase "i593_test"
            resolver[Names.CDLIB_PIPELINE_URL] shouldNotContainIgnoringCase "merge"
        }
    }

    @Test
    fun testCreate_PIPELINE_URL_sourceBranchName() {
        withEnvironment(
            mapOf(
                "BUILD_SOURCEBRANCHNAME" to "i593_test"
            ), OverrideMode.SetOrOverride
        ) {
            resolver[Names.CDLIB_PIPELINE_URL] shouldContainIgnoringCase "912345"
            resolver[Names.CDLIB_PIPELINE_URL] shouldContainIgnoringCase "i593_test"
        }
    }

    @Test
    fun testCreate_PM_VALUES() {
        resolver[Names.CDLIB_PM_GIT_ID] shouldBeEqualIgnoringCase "a5c5bc3ce1907e844490697b9aa22c4196c5d781"
        resolver[Names.CDLIB_PM_GIT_MAIL] shouldBeEqualIgnoringCase "f.c@dhl.com"
        resolver[Names.CDLIB_PM_GIT_MESSAGE] shouldBeEqualIgnoringCase "Dummy Commit"
        resolver[Names.CDLIB_PM_GIT_NAME] shouldBeEqualIgnoringCase "Firstname Committer"
        resolver[Names.CDLIB_PM_GIT_ORIGIN] shouldBeEqualIgnoringCase "https://git.dhl.com/CDLib/CDlib.git"
        resolver[Names.CDLIB_PM_GIT_LINK] shouldBeEqualIgnoringCase "https://git.dhl.com/CDLib/CDlib/commit/a5c5bc3ce1907e844490697b9aa22c4196c5d781"
    }

    @Test
    fun testCreate_RELEASE_VERSION() {
        resolver[Names.CDLIB_RELEASE_VERSION] shouldContainIgnoringCase "_12657_a5c5bc3"
    }

    @Test
    fun testCreate_RELEASE_NAME() {
        resolver[Names.CDLIB_RELEASE_NAME] shouldContainIgnoringCase "_12657_a5c5bc3"
        resolver[Names.CDLIB_RELEASE_NAME] shouldContainIgnoringCase "ICTO-3339_SDM-phippyandfriends"
    }

    @Test
    fun testCreate_RELEASE_NAME_FORTIFY() {
        resolver[Names.CDLIB_RELEASE_NAME_FORTIFY] shouldContainIgnoringCase "ICTO_3339_SDM_phippyandfriends"
    }

    @Test
    fun testCreate_RELEASE_NAME_HELM() {
        resolver[Names.CDLIB_RELEASE_NAME_HELM] shouldHaveMaxLength 53
        resolver[Names.CDLIB_RELEASE_NAME_HELM] shouldBeEqualIgnoringCase "ICTO-3339_SDM-phippyandfriends-i593-test"
    }

    @Test
    fun testCreate_REVISION() {
        resolver[Names.CDLIB_REVISION] shouldBeEqualComparingTo "a5c5bc3"
    }

    @Test
    fun testCreate_SANITIZED_BRANCH_NAME() {
        resolver[Names.CDLIB_SANITIZED_BRANCH_NAME] shouldBeEqualComparingTo "i593-test"
    }

    @Test
    fun testCreate_SEMANTIC_VERSION() {
        resolver[Names.CDLIB_SEMANTIC_VERSION] shouldContainIgnoringCase "-i593_test.12657.a5c5bc3"
    }

    @Test
    fun testCreate_TERRAFORM_PREFIX() {
        resolver[Names.CDLIB_TERRAFORM_PREFIX] shouldContainIgnoringCase "CDLib-i593-test"
    }

    @Test
    fun testCreate_RELEASE_NAME_UNIQUE() {
        val releaseNameUnique = resolver[Names.CDLIB_RELEASE_NAME_UNIQUE]
        releaseNameUnique shouldContain "ICTO-3339_SDM-phippyandfriends_"
        releaseNameUnique shouldContain "_12657_a5c5bc3"
        val date =
            ZonedDateTime.parse(releaseNameUnique.split("_").last(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        val after = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant()
        date shouldBeGreaterThanOrEqualTo before
        date shouldBeLessThanOrEqualTo after
    }

    @Test
    fun testCreate_releaseNameHelmShouldEndWithoutHypen() {
        withEnvironment(
            mapOf(
                "BUILD_DEFINITIONNAME" to "ITR-1337-dulli-daks-orchestrator",
                "BUILD_SOURCEBRANCHNAME" to "develop-update-flux-deployment",
            ),
            OverrideMode.SetOrOverride
        ) {
            val newResolver = NameResolverAzure(namesConfigWithDefault)
            newResolver[Names.CDLIB_RELEASE_NAME_HELM].last().isLetterOrDigit() shouldBe true
        }
    }

}
