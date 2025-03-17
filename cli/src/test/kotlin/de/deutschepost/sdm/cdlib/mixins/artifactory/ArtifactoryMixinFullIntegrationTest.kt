package de.deutschepost.sdm.cdlib.mixins.artifactory

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.io.File

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ArtifactoryMixinFullIntegrationTest(
    @Value("\${artifactory-azure-identity-token}") val artifactoryAzureIdentityToken: String
) : FunSpec() {
    private val artifactoryMixinFullAzure = ArtifactoryMixinFull().apply {
        artifactorySection.artifactoryIdentityToken =
            this@ArtifactoryMixinFullIntegrationTest.artifactoryAzureIdentityToken
        artifactorySection.immutableRepoName = "ICTO-3339_sdm_sockshop_nonimmutable_reports"
        artifactorySection.artifactoryInstanceSection.artifactoryAzureInstance = true
    }

    init {
        // Actual content of file in repo is azure.png so these are mixed up
        test("Existing file with same hash for azure artifactory is fine") {
            shouldNotThrowAny {
                val byteArray = File("src/test/resources/blobs/azure.png").readBytes()
                artifactoryMixinFullAzure.uploadToImmutable(byteArray, "foobar/kotlin.png")
            }
        }

        test("Existing file with different hash for azure artifactory is not fine") {
            shouldThrow<IllegalStateException> {
                val byteArray = File("src/test/resources/blobs/kotlin.png").readBytes()
                artifactoryMixinFullAzure.uploadToImmutable(byteArray, "foobar/kotlin.png")
            }
        }
    }
}
